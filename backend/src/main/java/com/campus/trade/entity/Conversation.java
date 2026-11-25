package com.campus.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Conversation {
    private Long id;
    private Long itemId;
    private Long buyerId;
    private Long sellerId;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
