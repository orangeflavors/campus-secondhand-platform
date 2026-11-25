package com.campus.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportVO {
    private Long id;
    private String reportNo;
    private String targetType;
    private Long targetId;
    private String targetTypeText;
    private String targetSummary;
    private String category;
    private String categoryText;
    private String description;
    private String status;
    private String statusText;
    private String handlingResult;
    private String ruleBasis;
    private String actionSummary;
    private String appealReason;
    private String handlerName;
    private String reporterNickname;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
