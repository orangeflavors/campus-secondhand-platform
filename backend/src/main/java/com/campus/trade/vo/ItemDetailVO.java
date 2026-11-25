package com.campus.trade.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 商品详情：在卡片信息基础上增加描述、图片、卖家资料与购买可用性判断。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ItemDetailVO extends ItemCardVO {
    private String description;
    private List<String> images;
    private Boolean isFavorite;
    private Boolean isOwner;
    private Boolean canOrder;
    private String cannotOrderReason;
    private String rejectReason;
    private PublicProfileVO seller;
    private String riskNotice;
    private Long activeOrderId;
}
