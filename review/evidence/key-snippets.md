# 关键代码片段（基线 8b593ea）

> 片段共 100 行（含空行），均来自基线提交 `8b593eada8b27c983545483a13b2311feddee079`。
> 路径均为项目根目录相对路径；行号与基线文件一致。

## 1. 接口入口与权限校验

`backend/src/main/java/com/campus/trade/controller/OrderController.java:52-57`

```java
    @PostMapping("/{id}/accept")
    public ApiResponse<OrderVO> accept(@PathVariable Long id) {
        UserPrincipal principal = SecurityUtils.requireVerified();
        SecurityUtils.requireNotBanned();
        return ApiResponse.ok(orderService.accept(id, principal.id()));
    }
```

## 2. 接受待确认请求的事务主体

`backend/src/main/java/com/campus/trade/service/OrderService.java:123-179`

```java
    @Transactional
    public OrderVO accept(Long orderId, Long sellerId) {
        TradeOrder snapshot = requireOrder(orderId);
        if (!snapshot.getSellerId().equals(sellerId)) {
            throw BizException.forbidden("只有卖家可以接受该请求");
        }
        if (!TradeConst.ORDER_PENDING_SELLER.equals(snapshot.getStatus())) {
            throw BizException.conflict("该请求已失效或已被处理");
        }
        // 固定加锁顺序：商品 → 订单，避免与并发接受产生死锁
        Item item = itemMapper.findByIdForUpdate(snapshot.getItemId());
        if (item == null) {
            throw BizException.conflict("商品不存在");
        }
        TradeOrder order = orderMapper.findByIdForUpdate(orderId);
        if (!TradeConst.ORDER_PENDING_SELLER.equals(order.getStatus())) {
            throw BizException.conflict("该请求已失效或已被处理");
        }
        if (!TradeConst.ITEM_ON_SALE.equals(item.getStatus())) {
            throw BizException.conflict("商品已被其他请求锁定或不在售，无法接受");
        }
        if (orderMapper.findActiveByItem(item.getId()) != null) {
            throw BizException.conflict("商品已被其他请求锁定，无法接受");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(TradeConst.ORDER_DELIVERING);
        order.setAcceptedAt(now);
        orderMapper.update(order);

        itemMapper.updateStatus(item.getId(), TradeConst.ITEM_LOCKED, item.getRejectReason(),
                item.getAuditBy(), item.getAuditAt(), item.getListedAt());

        HandoverRecord handover = new HandoverRecord();
        handover.setOrderId(orderId);
        handover.setUpdatedBy(sellerId);
        handoverMapper.insert(handover);

        // 同商品其他待确认请求 → 已取消（未中选）并通知（ST-O04a / BR-19）
        for (TradeOrder other : orderMapper.findOtherPending(item.getId(), orderId)) {
            other.setStatus(TradeConst.ORDER_CANCELLED);
            other.setCancelType(TradeConst.CANCEL_NOT_SELECTED);
            other.setCancelReason("卖家已接受其他买家的请求，本请求未中选");
            other.setClosedAt(now);
            orderMapper.update(other);
            notificationService.send(other.getBuyerId(), TradeConst.NOTIFY_ORDER, "待确认请求未中选",
                    "你申请的商品《" + item.getTitle() + "》已被其他买家抢先达成交易，本次请求未中选。",
                    "ORDER", other.getId());
        }

        notificationService.send(order.getBuyerId(), TradeConst.NOTIFY_ORDER, "卖家已接受你的请求",
                "卖家已接受你对《" + item.getTitle() + "》的请求，请与卖家约定校内线下交付时间与地点。",
                "ORDER", orderId);
        auditService.log("ORDER_ACCEPT", "ORDER", orderId, "卖家接受待确认请求",
                TradeConst.ORDER_PENDING_SELLER, TradeConst.ORDER_DELIVERING);
        return detailInternal(order, sellerId);
    }
```

## 3. 待确认请求的读取与订单更新

`backend/src/main/resources/mapper/OrderMapper.xml:44-48`

```xml
    <select id="findOtherPending" resultType="com.campus.trade.entity.TradeOrder">
        SELECT <include refid="orderColumns"/> FROM trade_order
        WHERE item_id = #{itemId} AND status = 'PENDING_SELLER' AND id &lt;&gt; #{excludeId}
        ORDER BY id
    </select>
```

`backend/src/main/resources/mapper/OrderMapper.xml:84-103`

```xml
    <update id="update">
        UPDATE trade_order
        <set>
            status = #{status},
            <if test="cancelType != null">cancel_type = #{cancelType},</if>
            <if test="cancelReason != null">cancel_reason = #{cancelReason},</if>
            <if test="cancelledBy != null">cancelled_by = #{cancelledBy},</if>
            <if test="acceptedAt != null">accepted_at = #{acceptedAt},</if>
            <if test="buyerConfirmedAt != null">buyer_confirmed_at = #{buyerConfirmedAt},</if>
            <if test="sellerConfirmedAt != null">seller_confirmed_at = #{sellerConfirmedAt},</if>
            <if test="completedAt != null">completed_at = #{completedAt},</if>
            <if test="closedAt != null">closed_at = #{closedAt},</if>
            <if test="expireAt != null">expire_at = #{expireAt},</if>
            <if test="disputeReason != null">dispute_reason = #{disputeReason},</if>
            <if test="disputedBy != null">disputed_by = #{disputedBy},</if>
            version = version + 1,
            updated_at = NOW()
        </set>
        WHERE id = #{id} AND version = #{version}
    </update>
```

## 4. 数据库一致性兜底

`database/schema.sql:196-207`

```sql
  active_item_key     BIGINT       GENERATED ALWAYS AS (
                        CASE WHEN status IN ('DELIVERING','BUYER_CONFIRMED','SELLER_CONFIRMED','DISPUTE')
                             THEN item_id ELSE NULL END
                      ) STORED COMMENT '活跃订单去重键',
  pending_buyer_key   VARCHAR(80)  GENERATED ALWAYS AS (
                        CASE WHEN status = 'PENDING_SELLER'
                             THEN CONCAT(item_id, '-', buyer_id) ELSE NULL END
                      ) STORED COMMENT '待确认请求去重键',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  UNIQUE KEY uk_order_active_item (active_item_key),
  UNIQUE KEY uk_order_pending_buyer (pending_buyer_key),
```
