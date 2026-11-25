package com.campus.trade.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端商品审核视图。
 */
@Data
public class AdminItemVO {
    private Long id;
    private String title;
    private String coverUrl;
    private BigDecimal price;
    private String categoryName;
    private Long sellerId;
    private String sellerNickname;
    private String sellerAuthStatus;
    private String status;
    private String statusText;
    private String rejectReason;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime auditAt;
    private LocalDateTime listedAt;
}
