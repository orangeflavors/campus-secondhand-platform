package com.campus.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户的完整资料（仅本人可见手机号、学号等敏感信息）。
 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String college;
    private String phone;
    private String email;
    private String studentNo;
    private String contactVisibility;
    private String role;
    private String accountStatus;
    private String authStatus;
    private String authMethod;
    private LocalDateTime authTime;
    private LocalDateTime limitedUntil;
    private String statusReason;
    private LocalDateTime createdAt;
    private UserStats stats;

    @Data
    public static class UserStats {
        private long publishedCount;
        private long soldCount;
        private long reviewCount;
        private double avgRating;
    }
}
