package com.campus.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HandoverRecord {
    private Long id;
    private Long orderId;
    private LocalDateTime handoverTime;
    private String area;
    private String note;
    private LocalDateTime buyerConfirmedAt;
    private LocalDateTime sellerConfirmedAt;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
