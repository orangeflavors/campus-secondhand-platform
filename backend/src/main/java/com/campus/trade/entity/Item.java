package com.campus.trade.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Item {
    private Long id;
    private Long sellerId;
    private String title;
    private Long categoryId;
    private String itemCondition;
    private String description;
    private BigDecimal price;
    private String handoverArea;
    private String status;
    private String rejectReason;
    private Long auditBy;
    private LocalDateTime auditAt;
    private LocalDateTime listedAt;
    private Integer viewCount;
    private Integer favoriteCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
