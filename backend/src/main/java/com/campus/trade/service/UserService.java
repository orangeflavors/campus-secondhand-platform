package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.common.ErrorCode;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.PageUtils;
import com.campus.trade.common.TradeConst;
import com.campus.trade.dto.AuthDtos;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.ReviewMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.PublicProfileVO;
import com.campus.trade.vo.ReviewVO;
import com.campus.trade.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户资料、校园认证与公开信息。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final ReviewMapper reviewMapper;
    private final AuditService auditService;

    public User requireUser(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return user;
    }

    public UserVO currentUserVO(Long userId) {
        return toUserVO(requireUser(userId));
    }

    public UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setCollege(user.getCollege());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStudentNo(user.getStudentNo());
        vo.setContactVisibility(user.getContactVisibility());
        vo.setRole(user.getRole());
        vo.setAccountStatus(user.getAccountStatus());
        vo.setAuthStatus(user.getAuthStatus());
        vo.setAuthMethod(user.getAuthMethod());
        vo.setAuthTime(user.getAuthTime());
        vo.setLimitedUntil(user.getLimitedUntil());
        vo.setStatusReason(user.getStatusReason());
        vo.setCreatedAt(user.getCreatedAt());

        UserVO.UserStats stats = new UserVO.UserStats();
        stats.setPublishedCount(userMapper.countPublishedItems(user.getId()));
        stats.setSoldCount(userMapper.countSoldItems(user.getId()));
        stats.setReviewCount(reviewMapper.countByTarget(user.getId()));
        stats.setAvgRating(round(reviewMapper.avgRatingByTarget(user.getId())));
        vo.setStats(stats);
        return vo;
    }

    /**
     * 他人可见资料：不暴露学号、真实姓名、邮箱；联系方式按可见范围决定。
     */
    public PublicProfileVO publicProfile(Long userId, Long viewerId) {
        User user = requireUser(userId);
        PublicProfileVO vo = new PublicProfileVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setCollege(user.getCollege());
        vo.setAuthStatus(user.getAuthStatus());
        vo.setMemberSince(user.getCreatedAt());
        vo.setAvgRating(round(reviewMapper.avgRatingByTarget(userId)));
        vo.setReviewCount(reviewMapper.countByTarget(userId));
        vo.setSoldCount(userMapper.countSoldItems(userId));
        vo.setContactPhone(visiblePhone(user, viewerId));
        return vo;
    }

    private String visiblePhone(User user, Long viewerId) {
        String visibility = user.getContactVisibility();
        if (TradeConst.CONTACT_PUBLIC.equals(visibility)) {
            return user.getPhone();
        }
        if (TradeConst.CONTACT_AUTHENTICATED.equals(visibility) && viewerId != null) {
            User viewer = userMapper.findById(viewerId);
            if (viewer != null && TradeConst.AUTH_VERIFIED.equals(viewer.getAuthStatus())) {
                return user.getPhone();
            }
        }
        return null;
    }

    @Transactional
    public UserVO updateProfile(Long userId, AuthDtos.ProfileUpdateRequest request) {
        User user = requireUser(userId);
        if (request.nickname() != null && !request.nickname().isBlank()) {
            user.setNickname(request.nickname().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().isBlank() ? null : request.phone().trim());
        }
        if (request.email() != null) {
            user.setEmail(request.email().isBlank() ? null : request.email().trim());
        }
        if (request.college() != null) {
            user.setCollege(request.college().isBlank() ? null : request.college().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl().trim());
        }
        if (request.contactVisibility() != null) {
            user.setContactVisibility(request.contactVisibility());
        }
        userMapper.updateProfile(user);
        auditService.log("USER_PROFILE_UPDATE", "USER", userId, "用户更新个人资料", null, user.getNickname());
        return currentUserVO(userId);
    }

    /**
     * 校园身份认证（模拟认证服务）：学历号格式校验通过即视为认证成功（Q-01 保留服务抽象）。
     */
    @Transactional
    public UserVO campusAuth(Long userId, AuthDtos.CampusAuthRequest request) {
        User user = requireUser(userId);
        if (TradeConst.AUTH_VERIFIED.equals(user.getAuthStatus())) {
            throw BizException.conflict("你已完成校园身份认证，无需重复提交");
        }
        User sameStudent = userMapper.findByStudentNo(request.studentNo());
        if (sameStudent != null && !sameStudent.getId().equals(userId)) {
            throw BizException.conflict("该学号已被其他账号绑定，请联系管理员");
        }
        String before = user.getAuthStatus();
        user.setStudentNo(request.studentNo());
        user.setRealName(request.realName().trim());
        user.setCollege(request.college().trim());
        user.setAuthStatus(TradeConst.AUTH_VERIFIED);
        user.setAuthMethod("STUDENT_NO");
        user.setAuthTime(java.time.LocalDateTime.now());
        userMapper.updateCampusAuth(user);
        auditService.log("USER_CAMPUS_AUTH", "USER", userId, "校园身份认证通过", before, TradeConst.AUTH_VERIFIED);
        return currentUserVO(userId);
    }

    public PageResult<ReviewVO> reviews(Long userId, Integer page, Integer size) {
        int p = PageUtils.page(page);
        int s = PageUtils.size(size, 5);
        List<ReviewVO> records = reviewMapper.listByTarget(userId, PageUtils.offset(p, s), s);
        return PageResult.of(records, reviewMapper.countByTarget(userId), p, s);
    }

    public User findById(Long id) {
        return userMapper.findById(id);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
