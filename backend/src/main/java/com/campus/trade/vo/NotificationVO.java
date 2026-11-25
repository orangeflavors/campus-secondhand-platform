package com.campus.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Long id;
    private String type;
    private String typeText;
    private String title;
    private String content;
    private String relatedType;
    private Long relatedId;
    private boolean read;
    private LocalDateTime createdAt;
}
