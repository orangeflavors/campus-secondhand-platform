package com.campus.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModerationActionVO {
    private Long id;
    private String targetType;
    private Long targetId;
    private String actionType;
    private String actionTypeText;
    private String reason;
    private String ruleBasis;
    private Integer durationDays;
    private String operatorName;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private LocalDateTime createdAt;
}
