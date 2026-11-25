package com.campus.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModerationAction {
    private Long id;
    private String targetType;
    private Long targetId;
    private String actionType;
    private Long operatorId;
    private String reason;
    private String ruleBasis;
    private Integer durationDays;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Long relatedReportId;
    private LocalDateTime createdAt;
}
