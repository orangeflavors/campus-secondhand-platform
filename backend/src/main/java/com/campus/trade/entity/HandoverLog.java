package com.campus.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HandoverLog {
    private Long id;
    private Long orderId;
    private Long changedBy;
    private String summary;
    private LocalDateTime createdAt;
}
