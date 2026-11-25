package com.campus.trade.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ConversationVO {
    private Long id;
    private Long itemId;
    private String itemTitle;
    private String itemCoverUrl;
    private BigDecimal itemPrice;
    private Long otherUserId;
    private String otherNickname;
    private String otherAvatarUrl;
    private String otherAuthStatus;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
