package com.campus.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItemImage {
    private Long id;
    private Long itemId;
    private String url;
    private Integer sortNo;
    private LocalDateTime createdAt;
}
