package com.campus.trade.security;

/**
 * 当前登录用户的安全上下文信息（每次请求从数据库加载，保证账户状态实时生效）。
 */
public record UserPrincipal(
        Long id,
        String username,
        String role,
        String accountStatus,
        String authStatus
) {
}
