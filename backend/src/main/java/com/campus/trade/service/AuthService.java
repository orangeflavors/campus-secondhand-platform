package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.common.TextUtils;
import com.campus.trade.common.TradeConst;
import com.campus.trade.dto.AuthDtos;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.security.JwtTokenProvider;
import com.campus.trade.vo.LoginResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 注册、登录、找回凭据（FR-001）。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    @Transactional
    public LoginResultVO register(AuthDtos.RegisterRequest request) {
        if (userMapper.findByUsername(request.username()) != null) {
            throw BizException.conflict("用户名已存在，请更换后重试");
        }
        String studentNo = TextUtils.isBlank(request.studentNo()) ? null : request.studentNo().trim();
        if (studentNo != null && userMapper.findByStudentNo(studentNo) != null) {
            throw BizException.conflict("该学号已被其他账号绑定");
        }
        User user = new User();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setCollege(TextUtils.isBlank(request.college()) ? null : request.college().trim());
        user.setPhone(TextUtils.isBlank(request.phone()) ? null : request.phone().trim());
        user.setEmail(TextUtils.isBlank(request.email()) ? null : request.email().trim());
        user.setStudentNo(studentNo);
        user.setContactVisibility(TradeConst.CONTACT_AUTHENTICATED);
        user.setRole(TradeConst.ROLE_USER);
        user.setAccountStatus(TradeConst.ACCOUNT_NORMAL);
        user.setAuthStatus(TradeConst.AUTH_UNVERIFIED);
        userMapper.insert(user);

        auditService.log("USER_REGISTER", "USER", user.getId(), "用户注册", null, user.getUsername());
        return issueToken(user);
    }

    public LoginResultVO login(AuthDtos.LoginRequest request) {
        User user = userMapper.findByUsername(request.username().trim());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        // 限权到期自动恢复
        if (TradeConst.ACCOUNT_LIMITED.equals(user.getAccountStatus())
                && user.getLimitedUntil() != null
                && user.getLimitedUntil().isBefore(LocalDateTime.now())) {
            userMapper.updateAccountStatus(user.getId(), TradeConst.ACCOUNT_NORMAL, null, "限权已到期自动恢复");
            user = userMapper.findById(user.getId());
        }
        return issueToken(user);
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        User user = userMapper.findByUsername(request.username().trim());
        if (user == null || user.getStudentNo() == null
                || !user.getStudentNo().equals(request.studentNo().trim())) {
            throw BizException.badRequest("用户名与学号不匹配，无法找回凭据");
        }
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()));
        auditService.log("USER_RESET_PASSWORD", "USER", user.getId(), "通过学号校验重置密码", null, null);
    }

    private LoginResultVO issueToken(User user) {
        String token = tokenProvider.generate(user.getId(), user.getUsername(), user.getRole());
        return new LoginResultVO(token, "Bearer", tokenProvider.getExpireSeconds(), userService.toUserVO(user));
    }

    /** 预留：注册时保留的字段合法性校验，便于扩展多认证方式。 */
    private static final Set<String> ALLOWED_CONTACT_VISIBILITY =
            Set.of(TradeConst.CONTACT_PUBLIC, TradeConst.CONTACT_AUTHENTICATED, TradeConst.CONTACT_PRIVATE);
}
