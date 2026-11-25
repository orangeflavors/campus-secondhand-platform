package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.common.TextUtils;
import com.campus.trade.common.TradeConst;
import com.campus.trade.entity.ModerationAction;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.ModerationActionMapper;
import com.campus.trade.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 账户处置：警告、限权、封禁与解封（FR-029 / BR-13）。
 */
@Service
@RequiredArgsConstructor
public class SanctionService {

    private final UserMapper userMapper;
    private final ModerationActionMapper moderationActionMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public ModerationAction apply(Long userId, String actionType, String reason, String ruleBasis,
                                  Integer durationDays, Long relatedReportId) {
        if (!TradeConst.USER_ACTIONS.contains(actionType)) {
            throw BizException.badRequest("账户处置动作不合法");
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if (TradeConst.ROLE_ADMIN.equals(user.getRole())) {
            throw BizException.forbidden("不能处置管理员账户");
        }
        if (TextUtils.isBlank(reason)) {
            throw BizException.badRequest("处置必须填写理由");
        }

        String before = user.getAccountStatus();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveTo = null;
        String newStatus = before;
        String statusReason = user.getStatusReason();
        String actionLabel;

        switch (actionType) {
            case TradeConst.ACTION_WARN -> actionLabel = "警告";
            case TradeConst.ACTION_LIMIT -> {
                actionLabel = "限权";
                newStatus = TradeConst.ACCOUNT_LIMITED;
                effectiveTo = now.plusDays(durationDays == null ? 7 : durationDays);
                statusReason = reason.trim();
            }
            case TradeConst.ACTION_BAN -> {
                actionLabel = "封禁";
                newStatus = TradeConst.ACCOUNT_BANNED;
                effectiveTo = durationDays == null ? null : now.plusDays(durationDays);
                statusReason = reason.trim();
            }
            case TradeConst.ACTION_UNBAN -> {
                actionLabel = "解封";
                newStatus = TradeConst.ACCOUNT_NORMAL;
                effectiveTo = null;
                statusReason = "账户限制已解除";
            }
            default -> throw BizException.badRequest("账户处置动作不合法");
        }

        if (!TradeConst.ACTION_WARN.equals(actionType)) {
            userMapper.updateAccountStatus(userId, newStatus, effectiveTo, statusReason);
        }

        ModerationAction action = new ModerationAction();
        action.setTargetType("USER");
        action.setTargetId(userId);
        action.setActionType(actionType);
        action.setOperatorId(com.campus.trade.security.SecurityUtils.currentId());
        action.setReason(reason.trim());
        action.setRuleBasis(ruleBasis);
        action.setDurationDays(durationDays);
        action.setEffectiveFrom(now);
        action.setEffectiveTo(effectiveTo);
        action.setRelatedReportId(relatedReportId);
        moderationActionMapper.insert(action);

        String notifyContent = switch (actionType) {
            case TradeConst.ACTION_WARN -> "你收到一次平台警告：" + reason.trim();
            case TradeConst.ACTION_LIMIT -> "你的账户被限制权限至 " + effectiveTo.toLocalDate()
                    + "，期间无法发布商品或创建订单。理由：" + reason.trim();
            case TradeConst.ACTION_BAN -> "你的账户已被封禁"
                    + (effectiveTo == null ? "" : "至 " + effectiveTo.toLocalDate()) + "。理由：" + reason.trim();
            default -> "你的账户限制已解除，可以正常使用平台功能。";
        };
        notificationService.send(userId, TradeConst.NOTIFY_ACCOUNT, "账户处置通知：" + actionLabel,
                notifyContent, null, action.getId());
        auditService.log("USER_" + actionType, "USER", userId,
                reason.trim() + (TextUtils.isBlank(ruleBasis) ? "" : "（依据：" + ruleBasis.trim() + "）"),
                before, newStatus);
        return action;
    }
}
