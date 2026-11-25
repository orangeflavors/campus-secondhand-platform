package com.campus.trade.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品卡片：列表与搜索结果的展示对象。
 */
@Data
public class ItemCardVO {
    private Long id;
    private String title;
    private String coverUrl;
    private BigDecimal price;
    private String condition;
    private String conditionText;
    private Long categoryId;
    private String categoryName;
    private String handoverArea;
    private String status;
    private String statusText;
    private Long sellerId;
    private String sellerNickname;
    private String sellerAuthStatus;
    private LocalDateTime listedAt;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private Integer favoriteCount;
}
