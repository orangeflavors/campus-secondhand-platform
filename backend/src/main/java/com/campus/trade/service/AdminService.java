package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.PageUtils;
import com.campus.trade.common.TextUtils;
import com.campus.trade.common.TradeConst;
import com.campus.trade.dto.AdminDtos;
import com.campus.trade.entity.AuditLog;
import com.campus.trade.entity.ModerationAction;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.AuditLogMapper;
import com.campus.trade.mapper.ModerationActionMapper;
import com.campus.trade.mapper.ReviewMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.AdminUserVO;
import com.campus.trade.vo.AuditLogVO;
import com.campus.trade.vo.ModerationActionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 管理端：用户检索、账户处置记录与审计日志（FR-029 / FR-031）。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Map<String, String> ACTION_TEXT = Map.ofEntries(
            Map.entry("ITEM_CREATE", "创建商品草稿"),
            Map.entry("ITEM_UPDATE", "编辑商品"),
            Map.entry("ITEM_SUBMIT", "提交商品审核"),
            Map.entry("ITEM_APPROVE", "商品审核通过"),
            Map.entry("ITEM_REJECT", "商品审核驳回"),
            Map.entry("ITEM_TAKEDOWN", "商品违规下架"),
            Map.entry("ITEM_OFF_SHELF", "卖家主动下架"),
            Map.entry("ITEM_DELETE", "删除商品"),
            Map.entry("ORDER_CREATE", "创建待确认请求"),
            Map.entry("ORDER_ACCEPT", "接受待确认请求"),
            Map.entry("ORDER_REJECT", "拒绝待确认请求"),
            Map.entry("ORDER_CANCEL", "取消订单"),
            Map.entry("ORDER_NOT_SELECTED", "待确认请求未中选"),
            Map.entry("ORDER_CONFIRM", "确认交付"),
            Map.entry("ORDER_COMPLETE", "订单完成"),
            Map.entry("ORDER_DISPUTE", "提交交易争议"),
            Map.entry("ORDER_DISPUTE_BY_TAKEDOWN", "商品下架转争议"),
            Map.entry("ORDER_DISPUTE_RESOLVE", "争议裁决"),
            Map.entry("ORDER_TIMEOUT", "待确认请求超时取消"),
            Map.entry("ORDER_REVIEW", "提交交易评价"),
            Map.entry("REPORT_CREATE", "提交举报"),
            Map.entry("REPORT_CLAIM", "领取举报"),
            Map.entry("REPORT_HANDLE", "处理举报"),
            Map.entry("REPORT_CLOSE", "关闭举报"),
            Map.entry("REPORT_APPEAL", "提交申诉"),
            Map.entry("REPORT_APPEAL_RESOLVE", "申诉复核"),
            Map.entry("USER_REGISTER", "用户注册"),
            Map.entry("USER_PROFILE_UPDATE", "更新个人资料"),
            Map.entry("USER_CAMPUS_AUTH", "校园身份认证"),
            Map.entry("USER_WARN", "账户警告"),
            Map.entry("USER_LIMIT", "账户限权"),
            Map.entry("USER_BAN", "账户封禁"),
            Map.entry("USER_UNBAN", "账户解封")
    );

    private final UserMapper userMapper;
    private final ModerationActionMapper moderationActionMapper;
    private final AuditLogMapper auditLogMapper;
    private final ReviewMapper reviewMapper;
    private final SanctionService sanctionService;

    public PageResult<AdminUserVO> users(String keyword, String accountStatus, String authStatus,
                                         Integer page, Integer size) {
        int p = PageUtils.page(page);
        int s = PageUtils.size(size, 10);
        String normalizedKeyword = TextUtils.isBlank(keyword) ? null : TextUtils.escapeLike(keyword.trim());
        List<User> users = userMapper.search(normalizedKeyword,
                TextUtils.isBlank(accountStatus) ? null : accountStatus,
                TextUtils.isBlank(authStatus) ? null : authStatus,
                PageUtils.offset(p, s), s);
        List<AdminUserVO> records = users.stream().map(this::toAdminUser).toList();
        long total = userMapper.countSearch(normalizedKeyword,
                TextUtils.isBlank(accountStatus) ? null : accountStatus,
                TextUtils.isBlank(authStatus) ? null : authStatus);
        return PageResult.of(records, total, p, s);
    }

    public AdminUserVO userDetail(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return toAdminUser(user);
    }

    public List<ModerationActionVO> userActions(Long userId) {
        List<ModerationActionVO> actions = moderationActionMapper.listByTarget("USER", userId);
        actions.forEach(action -> action.setActionTypeText(actionText(action.getActionType())));
        return actions;
    }

    public ModerationActionVO sanction(Long userId, Long adminId, AdminDtos.SanctionRequest request) {
        ModerationAction action = sanctionService.apply(userId, request.actionType(), request.reason(),
                request.ruleBasis(), request.durationDays(), null);
        User operator = userMapper.findById(adminId);
        ModerationActionVO vo = toActionVO(action);
        vo.setOperatorName(operator == null ? "管理员" : operator.getNickname());
        return vo;
    }

    public PageResult<AuditLogVO> auditLogs(String action, Integer page, Integer size) {
        int p = PageUtils.page(page);
        int s = PageUtils.size(size, 15);
        String keyword = TextUtils.isBlank(action) ? null : TextUtils.escapeLike(action.trim());
        List<AuditLogVO> records = auditLogMapper.list(keyword, PageUtils.offset(p, s), s);
        records.forEach(vo -> vo.setActionText(ACTION_TEXT.getOrDefault(vo.getAction(), vo.getAction())));
        long total = auditLogMapper.count(keyword);
        return PageResult.of(records, total, p, s);
    }

    private AdminUserVO toAdminUser(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setCollege(user.getCollege());
        vo.setRole(user.getRole());
        vo.setAuthStatus(user.getAuthStatus());
        vo.setAccountStatus(user.getAccountStatus());
        vo.setStatusReason(user.getStatusReason());
        vo.setLimitedUntil(user.getLimitedUntil());
        vo.setPhone(user.getPhone());
        vo.setStudentNo(user.getStudentNo());
        vo.setCreatedAt(user.getCreatedAt());

        UserVOStats stats = new UserVOStats();
        stats.setPublishedCount(userMapper.countPublishedItems(user.getId()));
        stats.setSoldCount(userMapper.countSoldItems(user.getId()));
        stats.setReviewCount(reviewMapper.countByTarget(user.getId()));
        stats.setAvgRating(Math.round(reviewMapper.avgRatingByTarget(user.getId()) * 10.0) / 10.0);
        vo.setStats(stats.toUserStats());
        return vo;
    }

    private ModerationActionVO toActionVO(ModerationAction action) {
        ModerationActionVO vo = new ModerationActionVO();
        vo.setId(action.getId());
        vo.setTargetType(action.getTargetType());
        vo.setTargetId(action.getTargetId());
        vo.setActionType(action.getActionType());
        vo.setActionTypeText(actionText(action.getActionType()));
        vo.setReason(action.getReason());
        vo.setRuleBasis(action.getRuleBasis());
        vo.setDurationDays(action.getDurationDays());
        vo.setEffectiveFrom(action.getEffectiveFrom());
        vo.setEffectiveTo(action.getEffectiveTo());
        vo.setCreatedAt(action.getCreatedAt());
        return vo;
    }

    private String actionText(String actionType) {
        if (actionType == null) {
            return "—";
        }
        return switch (actionType) {
            case TradeConst.ACTION_TAKEDOWN -> "违规下架";
            case TradeConst.ACTION_WARN -> "警告";
            case TradeConst.ACTION_LIMIT -> "限权";
            case TradeConst.ACTION_BAN -> "封禁";
            case TradeConst.ACTION_UNBAN -> "解封";
            default -> actionType;
        };
    }

    /** 临时统计载体，避免在服务中重复构造嵌套类。 */
    private static final class UserVOStats {
        private long publishedCount;
        private long soldCount;
        private long reviewCount;
        private double avgRating;

        void setPublishedCount(long publishedCount) {
            this.publishedCount = publishedCount;
        }

        void setSoldCount(long soldCount) {
            this.soldCount = soldCount;
        }

        void setReviewCount(long reviewCount) {
            this.reviewCount = reviewCount;
        }

        void setAvgRating(double avgRating) {
            this.avgRating = avgRating;
        }

        com.campus.trade.vo.UserVO.UserStats toUserStats() {
            com.campus.trade.vo.UserVO.UserStats stats = new com.campus.trade.vo.UserVO.UserStats();
            stats.setPublishedCount(publishedCount);
            stats.setSoldCount(soldCount);
            stats.setReviewCount(reviewCount);
            stats.setAvgRating(avgRating);
            return stats;
        }
    }
}
