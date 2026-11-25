package com.campus.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notification {
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String relatedType;
    private Long relatedId;
    private Integer readFlag;
    private String sendStatus;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
