package com.campus.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewVO {
    private Long id;
    private Long orderId;
    private Long authorId;
    private String authorNickname;
    private String authorAvatarUrl;
    private Long targetId;
    private String targetNickname;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
