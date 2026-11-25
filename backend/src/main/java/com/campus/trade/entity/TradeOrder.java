package com.campus.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TradeOrder {
    private Long id;
    private String orderNo;
    private Long itemId;
    private Long buyerId;
    private Long sellerId;
    private String status;
    private String requestRemark;
    private String cancelType;
    private String cancelReason;
    private Long cancelledBy;
    private LocalDateTime acceptedAt;
    private LocalDateTime buyerConfirmedAt;
    private LocalDateTime sellerConfirmedAt;
    private LocalDateTime completedAt;
    private LocalDateTime closedAt;
    private LocalDateTime expireAt;
    private String disputeReason;
    private Long disputedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
