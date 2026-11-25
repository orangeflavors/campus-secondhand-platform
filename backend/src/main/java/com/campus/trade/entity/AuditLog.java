package com.campus.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLog {
    private Long id;
    private Long operatorId;
    private String operatorName;
    private String action;
    private String targetType;
    private Long targetId;
    private String reason;
    private String beforeValue;
    private String afterValue;
    private String ip;
    private LocalDateTime createdAt;
}
