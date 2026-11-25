package com.campus.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String college;
    private String role;
    private String authStatus;
    private String accountStatus;
    private String statusReason;
    private LocalDateTime limitedUntil;
    private String phone;
    private String studentNo;
    private LocalDateTime createdAt;
    private UserVO.UserStats stats;
}
