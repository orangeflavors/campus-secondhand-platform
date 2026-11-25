package com.campus.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogVO {
    private Long id;
    private String operatorName;
    private String action;
    private String actionText;
    private String targetType;
    private Long targetId;
    private String reason;
    private String beforeValue;
    private String afterValue;
    private LocalDateTime createdAt;
}
