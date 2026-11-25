package com.campus.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HandoverLogVO {
    private Long id;
    private Long changedBy;
    private String changedByName;
    private String summary;
    private LocalDateTime createdAt;
}
