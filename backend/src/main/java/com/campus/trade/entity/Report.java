package com.campus.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Report {
    private Long id;
    private String reportNo;
    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String category;
    private String description;
    private String snapshot;
    private String status;
    private Long handlerId;
    private String handlingResult;
    private String ruleBasis;
    private String actionSummary;
    private String appealReason;
    private LocalDateTime claimedAt;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
