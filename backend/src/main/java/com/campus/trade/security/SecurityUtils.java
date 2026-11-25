package com.campus.trade.security;

import com.campus.trade.common.BizException;
import com.campus.trade.common.ErrorCode;
import com.campus.trade.common.TradeConst;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 服务端权限校验工具。所有受保护操作必须调用，页面隐藏按钮不能替代鉴权（NFR-04）。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        return principal;
    }

    public static Long currentId() {
        return current().id();
    }

    public static boolean isAdmin() {
        return TradeConst.ROLE_ADMIN.equals(current().role());
    }

    /** 要求已完成校园认证（FR-002 / BR-01）。 */
    public static UserPrincipal requireVerified() {
        UserPrincipal principal = current();
        if (!TradeConst.AUTH_VERIFIED.equals(principal.authStatus())) {
            throw new BizException(ErrorCode.NEED_VERIFIED, "请先完成校园身份认证后再进行操作");
        }
        return principal;
    }

    /** 要求账户未被封禁。 */
    public static UserPrincipal requireNotBanned() {
        UserPrincipal principal = current();
        if (TradeConst.ACCOUNT_BANNED.equals(principal.accountStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_RESTRICTED, "账户已被封禁，无法执行该操作");
        }
        return principal;
    }

    /** 要求可参与交易（已认证、未封禁、未限权）。用于发布商品、创建请求等。 */
    public static UserPrincipal requireTradeable() {
        UserPrincipal principal = requireVerified();
        if (TradeConst.ACCOUNT_BANNED.equals(principal.accountStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_RESTRICTED, "账户已被封禁，无法参与交易");
        }
        if (TradeConst.ACCOUNT_LIMITED.equals(principal.accountStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_RESTRICTED, "账户处于限权状态，暂时无法发布商品或创建订单");
        }
        return principal;
    }

    /** 要求可发送消息（已认证且未封禁，限权不影响沟通）。 */
    public static UserPrincipal requireMessaging() {
        return requireNotBanned();
    }

    public static UserPrincipal requireAdmin() {
        UserPrincipal principal = current();
        if (!TradeConst.ROLE_ADMIN.equals(principal.role())) {
            throw new BizException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
        return principal;
    }

    public static void requireOwner(Long ownerId, String message) {
        if (!ownerId.equals(currentId())) {
            throw new BizException(ErrorCode.FORBIDDEN, message);
        }
    }
}
