package com.campus.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 他人可见的公开资料：不包含学号、真实姓名、邮箱等敏感信息。
 */
@Data
public class PublicProfileVO {
    private Long id;
    private String nickname;
    private String avatarUrl;
    private String college;
    private String authStatus;
    private LocalDateTime memberSince;
    private double avgRating;
    private long reviewCount;
    private long soldCount;
    private String contactPhone;
}
