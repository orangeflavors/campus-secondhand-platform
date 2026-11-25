package com.campus.trade.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图：包含买卖双方、商品摘要、交付约定、评价与当前用户可执行的动作。
 */
@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private String status;
    private String statusText;
    private ItemBrief item;
    private UserBrief buyer;
    private UserBrief seller;
    private String myRole;
    private String requestRemark;
    private String cancelType;
    private String cancelTypeText;
    private String cancelReason;
    private String disputeReason;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime expireAt;
    private LocalDateTime buyerConfirmedAt;
    private LocalDateTime sellerConfirmedAt;
    private LocalDateTime completedAt;
    private Handover handover;
    private List<ReviewVO> reviews;
    private ReviewVO myReview;
    private Actions actions;

    @Data
    public static class ItemBrief {
        private Long id;
        private String title;
        private String coverUrl;
        private BigDecimal price;
    }

    @Data
    public static class UserBrief {
        private Long id;
        private String nickname;
        private String avatarUrl;
    }

    @Data
    public static class Handover {
        private LocalDateTime handoverTime;
        private String area;
        private String note;
        private LocalDateTime buyerConfirmedAt;
        private LocalDateTime sellerConfirmedAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class Actions {
        private boolean canAccept;
        private boolean canReject;
        private boolean canCancel;
        private boolean canConfirm;
        private boolean canDispute;
        private boolean canReview;
        private boolean canEditHandover;
    }
}
