package com.campus.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String phone;
    private String email;
    private String realName;
    private String studentNo;
    private String college;
    private String avatarUrl;
    private String contactVisibility;
    private String role;
    private String accountStatus;
    private String authStatus;
    private String authMethod;
    private LocalDateTime authTime;
    private LocalDateTime limitedUntil;
    private String statusReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
