package com.campus.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Review {
    private Long id;
    private Long orderId;
    private Long authorId;
    private Long targetId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
