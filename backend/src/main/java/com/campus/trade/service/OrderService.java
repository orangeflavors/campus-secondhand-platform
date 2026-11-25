package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.common.TextUtils;
import com.campus.trade.common.TradeConst;
import com.campus.trade.dto.OrderDtos;
import com.campus.trade.entity.HandoverLog;
import com.campus.trade.entity.HandoverRecord;
import com.campus.trade.entity.Item;
import com.campus.trade.entity.ItemImage;
import com.campus.trade.entity.Review;
import com.campus.trade.entity.TradeOrder;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.HandoverMapper;
import com.campus.trade.mapper.ItemImageMapper;
import com.campus.trade.mapper.ItemMapper;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ReviewMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.HandoverLogVO;
import com.campus.trade.vo.OrderVO;
import com.campus.trade.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 待确认请求、订单与线下交付（FR-017 ~ FR-025 / ST-O01 ~ ST-O12）。
 * 关键一致性：同一商品最多 1 条已接受且未终结的订单；接受请求与锁定商品在同一事务内完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final HandoverMapper handoverMapper;
    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Value("${app.order.timeout-hours:24}")
    private int timeoutHours;

    @Value("${app.order.max-pending-per-item:10}")
    private int maxPendingPerItem;

    @Value("${app.review.window-days:7}")
    private int reviewWindowDays;

    /* ------------------------------ 创建待确认请求 ------------------------------ */

    @Transactional
    public OrderVO create(Long buyerId, Long itemId, String remark) {
        // 锁定商品行，串行化同一商品的并发请求计数（BR-19）
        Item item = itemMapper.findByIdForUpdate(itemId);
        if (item == null) {
            throw BizException.notFound("商品不存在");
        }
        if (!TradeConst.ITEM_ON_SALE.equals(item.getStatus())) {
            throw BizException.conflict("商品当前不在售，无法创建待确认请求");
        }
        if (item.getSellerId().equals(buyerId)) {
            throw BizException.conflict("不能购买自己发布的商品");
        }
        User buyer = userMapper.findById(buyerId);
        if (buyer == null) {
            throw BizException.unauthorized("登录状态异常，请重新登录");
        }
        if (TradeConst.ACCOUNT_BANNED.equals(buyer.getAccountStatus())) {
            throw BizException.forbidden("账户已被封禁，无法创建待确认请求");
        }
        if (TradeConst.ACCOUNT_LIMITED.equals(buyer.getAccountStatus())) {
            throw BizException.forbidden("账户处于限权状态，暂时无法创建订单");
        }
        if (!TradeConst.AUTH_VERIFIED.equals(buyer.getAuthStatus())) {
            throw BizException.forbidden("请先完成校园身份认证");
        }
        if (orderMapper.findPendingByItemAndBuyer(itemId, buyerId) != null) {
            throw BizException.conflict("你已提交过该商品的待确认请求，请等待卖家处理");
        }
        long pending = orderMapper.countPendingByItem(itemId);
        if (pending >= maxPendingPerItem) {
            throw BizException.conflict("该商品的待确认请求已达上限（" + maxPendingPerItem + " 条），请稍后再试");
        }

        LocalDateTime now = LocalDateTime.now();
        TradeOrder order = new TradeOrder();
        order.setOrderNo(TextUtils.generateNo("O"));
        order.setItemId(itemId);
        order.setBuyerId(buyerId);
        order.setSellerId(item.getSellerId());
        order.setStatus(TradeConst.ORDER_PENDING_SELLER);
        order.setRequestRemark(TextUtils.trimTo(remark, 255));
        order.setExpireAt(now.plusHours(timeoutHours));
        orderMapper.insert(order);

        notificationService.send(item.getSellerId(), TradeConst.NOTIFY_ORDER, "收到新的待确认请求",
                buyer.getNickname() + " 对你的商品《" + item.getTitle() + "》提交了待确认请求，请及时处理（"
                        + timeoutHours + " 小时内未处理将自动取消）。", "ORDER", order.getId());
        auditService.log("ORDER_CREATE", "ORDER", order.getId(), "买家创建待确认请求", null, order.getOrderNo());
        return detailInternal(order, buyerId);
    }

    /* ------------------------------ 卖家接受 / 拒绝 ------------------------------ */

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

    @Transactional
    public OrderVO reject(Long orderId, Long sellerId, String reason) {
        TradeOrder order = orderMapper.findByIdForUpdate(orderId);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        if (!order.getSellerId().equals(sellerId)) {
            throw BizException.forbidden("只有卖家可以拒绝该请求");
        }
        if (!TradeConst.ORDER_PENDING_SELLER.equals(order.getStatus())) {
            throw BizException.conflict("该请求已失效或已被处理");
        }
        if (TextUtils.isBlank(reason)) {
            throw BizException.badRequest("请填写拒绝原因");
        }
        order.setStatus(TradeConst.ORDER_CANCELLED);
        order.setCancelType(TradeConst.CANCEL_SELLER_REJECT);
        order.setCancelReason(reason.trim());
        order.setCancelledBy(sellerId);
        order.setClosedAt(LocalDateTime.now());
        orderMapper.update(order);

        notificationService.send(order.getBuyerId(), TradeConst.NOTIFY_ORDER, "卖家拒绝了你的请求",
                "卖家拒绝了你对商品请求的原因：" + reason.trim() + "。你可以继续浏览其他商品。", "ORDER", orderId);
        auditService.log("ORDER_REJECT", "ORDER", orderId, reason.trim(),
                TradeConst.ORDER_PENDING_SELLER, TradeConst.ORDER_CANCELLED);
        return detailInternal(order, sellerId);
    }

    /* ------------------------------ 取消 ------------------------------ */

    @Transactional
    public OrderVO cancel(Long orderId, Long userId, String reason) {
        TradeOrder order = orderMapper.findByIdForUpdate(orderId);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        boolean buyer = order.getBuyerId().equals(userId);
        boolean seller = order.getSellerId().equals(userId);
        if (!buyer && !seller) {
            throw BizException.forbidden("无权操作该订单");
        }
        if (TextUtils.isBlank(reason)) {
            throw BizException.badRequest("请填写取消原因");
        }

        String status = order.getStatus();
        if (TradeConst.ORDER_PENDING_SELLER.equals(status)) {
            if (!buyer) {
                throw BizException.conflict("待确认阶段卖家请使用「拒绝」");
            }
            order.setStatus(TradeConst.ORDER_CANCELLED);
            order.setCancelType(TradeConst.CANCEL_BUYER);
            order.setCancelReason(reason.trim());
            order.setCancelledBy(userId);
            order.setClosedAt(LocalDateTime.now());
            orderMapper.update(order);
            notifyCancel(order, userId, "买家取消了待确认请求");
        } else if (TradeConst.ORDER_DELIVERING.equals(status)) {
            if (order.getBuyerConfirmedAt() != null || order.getSellerConfirmedAt() != null) {
                throw BizException.conflict("已有一方确认交付，不能普通取消，请提交争议");
            }
            order.setStatus(TradeConst.ORDER_CANCELLED);
            order.setCancelType(TradeConst.CANCEL_BUYER);
            order.setCancelReason(reason.trim());
            order.setCancelledBy(userId);
            order.setClosedAt(LocalDateTime.now());
            orderMapper.update(order);
            restoreItemOnSale(order.getItemId());
            notifyCancel(order, userId, "订单已取消，商品恢复在售");
        } else if (TradeConst.ORDER_BUYER_CONFIRMED.equals(status) || TradeConst.ORDER_SELLER_CONFIRMED.equals(status)) {
            throw BizException.conflict("已有一方确认交付，不能普通取消，请提交争议");
        } else if (TradeConst.ORDER_DISPUTE.equals(status)) {
            throw BizException.conflict("订单处于争议中，不能取消");
        } else {
            throw BizException.conflict("当前订单状态不支持取消");
        }

        auditService.log("ORDER_CANCEL", "ORDER", orderId, reason.trim(), status, order.getStatus());
        return detailInternal(order, userId);
    }

    private void notifyCancel(TradeOrder order, Long operatorId, String message) {
        Long target = order.getBuyerId().equals(operatorId) ? order.getSellerId() : order.getBuyerId();
        notificationService.send(target, TradeConst.NOTIFY_ORDER, "订单状态更新", message, "ORDER", order.getId());
    }

    private void restoreItemOnSale(Long itemId) {
        Item item = itemMapper.findByIdForUpdate(itemId);
        if (item != null && TradeConst.ITEM_LOCKED.equals(item.getStatus())) {
            itemMapper.updateStatus(itemId, TradeConst.ITEM_ON_SALE, item.getRejectReason(),
                    item.getAuditBy(), item.getAuditAt(), item.getListedAt() == null ? LocalDateTime.now() : item.getListedAt());
        }
    }

    /* ------------------------------ 线下交付 ------------------------------ */

    @Transactional
    public OrderVO updateHandover(Long orderId, Long userId, OrderDtos.HandoverRequest request) {
        TradeOrder order = requireOrder(orderId);
        requireParticipant(order, userId);
        if (!Set.of(TradeConst.ORDER_DELIVERING, TradeConst.ORDER_BUYER_CONFIRMED,
                TradeConst.ORDER_SELLER_CONFIRMED).contains(order.getStatus())) {
            throw BizException.conflict("当前订单状态不允许修改交付约定");
        }
        HandoverRecord handover = handoverMapper.findByOrderId(orderId);
        if (handover == null) {
            handover = new HandoverRecord();
            handover.setOrderId(orderId);
            handover.setUpdatedBy(userId);
            handoverMapper.insert(handover);
            handover = handoverMapper.findByOrderId(orderId);
        }

        List<String> changes = new ArrayList<>();
        if (request.handoverTime() != null) {
            changes.add("交付时间：" + text(handover.getHandoverTime()) + " → " + request.handoverTime());
            handover.setHandoverTime(request.handoverTime());
        }
        if (request.area() != null) {
            changes.add("交付区域：" + text(handover.getArea()) + " → " + text(request.area()));
            handover.setArea(request.area().trim());
        }
        if (request.note() != null) {
            changes.add("备注：" + text(handover.getNote()) + " → " + text(request.note()));
            handover.setNote(request.note().trim());
        }
        if (changes.isEmpty()) {
            throw BizException.badRequest("请至少修改一项交付约定");
        }
        handover.setUpdatedBy(userId);
        handoverMapper.update(handover);

        HandoverLog log = new HandoverLog();
        log.setOrderId(orderId);
        log.setChangedBy(userId);
        log.setSummary(String.join("；", changes));
        handoverMapper.insertLog(log);

        Long other = order.getBuyerId().equals(userId) ? order.getSellerId() : order.getBuyerId();
        notificationService.send(other, TradeConst.NOTIFY_ORDER, "交付约定已更新",
                "对方更新了交付约定：" + String.join("；", changes), "ORDER", orderId);
        auditService.log("ORDER_HANDOVER_UPDATE", "ORDER", orderId, String.join("；", changes), null, null);
        return detailInternal(order, userId);
    }

    @Transactional
    public OrderVO confirm(Long orderId, Long userId) {
        TradeOrder snapshot = requireOrder(orderId);
        requireParticipant(snapshot, userId);
        Item item = itemMapper.findByIdForUpdate(snapshot.getItemId());
        if (item == null) {
            throw BizException.conflict("商品不存在");
        }
        TradeOrder order = orderMapper.findByIdForUpdate(orderId);
        boolean buyer = order.getBuyerId().equals(userId);
        LocalDateTime now = LocalDateTime.now();
        String status = order.getStatus();

        if (TradeConst.ORDER_DELIVERING.equals(status)) {
            if (buyer) {
                order.setBuyerConfirmedAt(now);
                order.setStatus(TradeConst.ORDER_BUYER_CONFIRMED);
            } else {
                order.setSellerConfirmedAt(now);
                order.setStatus(TradeConst.ORDER_SELLER_CONFIRMED);
            }
            orderMapper.update(order);
            confirmHandover(orderId, buyer, now);
            Long other = buyer ? order.getSellerId() : order.getBuyerId();
            notificationService.send(other, TradeConst.NOTIFY_ORDER, "对方已确认交付",
                    "对方已确认完成线下交付，请确认收到商品后及时确认，双方均确认后订单完成。", "ORDER", orderId);
            auditService.log("ORDER_CONFIRM", "ORDER", orderId, "一方确认交付", status, order.getStatus());
        } else if (TradeConst.ORDER_BUYER_CONFIRMED.equals(status)) {
            if (buyer) {
                return detailInternal(order, userId);
            }
            order.setSellerConfirmedAt(now);
            order.setStatus(TradeConst.ORDER_COMPLETED);
            order.setCompletedAt(now);
            orderMapper.update(order);
            confirmHandover(orderId, false, now);
            itemMapper.updateStatus(item.getId(), TradeConst.ITEM_SOLD, item.getRejectReason(),
                    item.getAuditBy(), item.getAuditAt(), item.getListedAt());
            notifyCompleted(order, item);
            auditService.log("ORDER_COMPLETE", "ORDER", orderId, "双方确认交付，订单完成",
                    TradeConst.ORDER_BUYER_CONFIRMED, TradeConst.ORDER_COMPLETED);
        } else if (TradeConst.ORDER_SELLER_CONFIRMED.equals(status)) {
            if (!buyer) {
                return detailInternal(order, userId);
            }
            order.setBuyerConfirmedAt(now);
            order.setStatus(TradeConst.ORDER_COMPLETED);
            order.setCompletedAt(now);
            orderMapper.update(order);
            confirmHandover(orderId, true, now);
            itemMapper.updateStatus(item.getId(), TradeConst.ITEM_SOLD, item.getRejectReason(),
                    item.getAuditBy(), item.getAuditAt(), item.getListedAt());
            notifyCompleted(order, item);
            auditService.log("ORDER_COMPLETE", "ORDER", orderId, "双方确认交付，订单完成",
                    TradeConst.ORDER_SELLER_CONFIRMED, TradeConst.ORDER_COMPLETED);
        } else if (TradeConst.ORDER_COMPLETED.equals(status)) {
            return detailInternal(order, userId);
        } else if (TradeConst.ORDER_DISPUTE.equals(status)) {
            throw BizException.conflict("订单处于争议中，不能直接确认交付");
        } else {
            throw BizException.conflict("当前订单状态不允许确认交付");
        }
        return detailInternal(order, userId);
    }

    private void confirmHandover(Long orderId, boolean buyer, LocalDateTime now) {
        HandoverRecord handover = handoverMapper.findByOrderId(orderId);
        if (handover == null) {
            handover = new HandoverRecord();
            handover.setOrderId(orderId);
            handover.setUpdatedBy(null);
            handoverMapper.insert(handover);
            handover = handoverMapper.findByOrderId(orderId);
        }
        if (buyer) {
            handover.setBuyerConfirmedAt(now);
        } else {
            handover.setSellerConfirmedAt(now);
        }
        handoverMapper.update(handover);
    }

    private void notifyCompleted(TradeOrder order, Item item) {
        notificationService.send(order.getBuyerId(), TradeConst.NOTIFY_ORDER, "交易已完成",
                "你与卖家的《" + item.getTitle() + "》交易已完成，欢迎在 " + reviewWindowDays + " 天内评价对方。",
                "ORDER", order.getId());
        notificationService.send(order.getSellerId(), TradeConst.NOTIFY_ORDER, "交易已完成",
                "你与买家的《" + item.getTitle() + "》交易已完成，欢迎在 " + reviewWindowDays + " 天内评价对方。",
                "ORDER", order.getId());
    }

    @Transactional
    public OrderVO dispute(Long orderId, Long userId, String reason) {
        TradeOrder order = orderMapper.findByIdForUpdate(orderId);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        requireParticipant(order, userId);
        if (!Set.of(TradeConst.ORDER_DELIVERING, TradeConst.ORDER_BUYER_CONFIRMED,
                TradeConst.ORDER_SELLER_CONFIRMED).contains(order.getStatus())) {
            throw BizException.conflict("当前订单状态不能提交争议");
        }
        if (TextUtils.isBlank(reason)) {
            throw BizException.badRequest("请填写争议说明");
        }
        String before = order.getStatus();
        order.setStatus(TradeConst.ORDER_DISPUTE);
        order.setDisputeReason(reason.trim());
        order.setDisputedBy(userId);
        orderMapper.update(order);

        Long other = order.getBuyerId().equals(userId) ? order.getSellerId() : order.getBuyerId();
        notificationService.send(other, TradeConst.NOTIFY_ORDER, "对方提交了交易争议",
                "订单进入争议处理，平台管理员将介入核实： " + reason.trim(), "ORDER", orderId);
        auditService.log("ORDER_DISPUTE", "ORDER", orderId, reason.trim(), before, TradeConst.ORDER_DISPUTE);
        return detailInternal(order, userId);
    }

    /* ------------------------------ 管理端 ------------------------------ */

    @Transactional
    public void onItemTakedown(Item item, String reason, Long adminId) {
        TradeOrder active = orderMapper.findActiveByItem(item.getId());
        if (active == null || TradeConst.ORDER_DISPUTE.equals(active.getStatus())) {
            return;
        }
        String before = active.getStatus();
        active.setStatus(TradeConst.ORDER_DISPUTE);
        active.setDisputeReason("商品被管理员违规下架：" + reason);
        active.setDisputedBy(adminId);
        orderMapper.update(active);
        notificationService.send(active.getBuyerId(), TradeConst.NOTIFY_ORDER, "相关订单进入争议处理",
                "你申请的商品《" + item.getTitle() + "》因违规被下架，订单已转人工处理，请等待管理员核实。",
                "ORDER", active.getId());
        notificationService.send(active.getSellerId(), TradeConst.NOTIFY_ORDER, "相关订单进入争议处理",
                "商品《" + item.getTitle() + "》因违规被下架，相关订单已转人工处理。", "ORDER", active.getId());
        auditService.log("ORDER_DISPUTE_BY_TAKEDOWN", "ORDER", active.getId(),
                "商品违规下架导致订单转争议", before, TradeConst.ORDER_DISPUTE);
    }

    @Transactional
    public OrderVO adminResolveDispute(Long orderId, String outcome, String reason, Long adminId) {
        TradeOrder order = orderMapper.findByIdForUpdate(orderId);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        if (!TradeConst.ORDER_DISPUTE.equals(order.getStatus())) {
            throw BizException.conflict("只有争议中的订单可以裁决");
        }
        if (!"COMPLETE".equals(outcome) && !"CLOSE".equals(outcome)) {
            throw BizException.badRequest("裁决结果不合法");
        }
        if (TextUtils.isBlank(reason)) {
            throw BizException.badRequest("请填写裁决理由");
        }
        String before = order.getStatus();
        LocalDateTime now = LocalDateTime.now();
        Item item = itemMapper.findById(order.getItemId());
        if ("COMPLETE".equals(outcome)) {
            order.setStatus(TradeConst.ORDER_COMPLETED);
            order.setCompletedAt(now);
            orderMapper.update(order);
            if (item != null) {
                itemMapper.updateStatus(item.getId(), TradeConst.ITEM_SOLD, item.getRejectReason(),
                        item.getAuditBy(), item.getAuditAt(), item.getListedAt());
            }
        } else {
            order.setStatus(TradeConst.ORDER_MANAGEMENT_CLOSED);
            order.setClosedAt(now);
            orderMapper.update(order);
            if (item != null && !TradeConst.ITEM_VIOLATED.equals(item.getStatus())) {
                itemMapper.updateStatus(item.getId(), TradeConst.ITEM_OFF_SHELF, item.getRejectReason(),
                        item.getAuditBy(), item.getAuditAt(), null);
            }
        }
        String label = "COMPLETE".equals(outcome) ? "订单完成" : "订单管理关闭";
        notificationService.send(order.getBuyerId(), TradeConst.NOTIFY_ORDER, "争议订单已裁决",
                "管理员已完成争议裁决：" + label + "。理由：" + reason.trim(), "ORDER", orderId);
        notificationService.send(order.getSellerId(), TradeConst.NOTIFY_ORDER, "争议订单已裁决",
                "管理员已完成争议裁决：" + label + "。理由：" + reason.trim(), "ORDER", orderId);
        auditService.log("ORDER_DISPUTE_RESOLVE", "ORDER", orderId, reason.trim(), before, order.getStatus());
        return detailInternal(order, adminId);
    }

    public List<OrderVO> listDisputes(Integer page, Integer size) {
        int p = com.campus.trade.common.PageUtils.page(page);
        int s = com.campus.trade.common.PageUtils.size(size, 10);
        List<TradeOrder> orders = orderMapper.listDisputes(com.campus.trade.common.PageUtils.offset(p, s), s);
        return buildOrders(orders, null);
    }

    public long countDisputes() {
        return orderMapper.countDisputes();
    }

    /* ------------------------------ 查询 ------------------------------ */

    public List<OrderVO> list(Long userId, String role, String status) {
        String normalizedRole = TextUtils.isBlank(role) ? null : role.toUpperCase();
        if (normalizedRole != null && !"BUYER".equals(normalizedRole) && !"SELLER".equals(normalizedRole)) {
            throw BizException.badRequest("角色参数不合法");
        }
        List<TradeOrder> orders = orderMapper.listByUser(userId, normalizedRole,
                TextUtils.isBlank(status) ? null : status);
        return buildOrders(orders, userId);
    }

    public OrderVO detail(Long orderId, Long userId, boolean admin) {
        TradeOrder order = requireOrder(orderId);
        if (!admin) {
            requireParticipant(order, userId);
        }
        return detailInternal(order, userId);
    }

    public List<HandoverLogVO> handoverLogs(Long orderId, Long userId, boolean admin) {
        TradeOrder order = requireOrder(orderId);
        if (!admin) {
            requireParticipant(order, userId);
        }
        return handoverMapper.listLogs(orderId);
    }

    private OrderVO detailInternal(TradeOrder order, Long viewerId) {
        List<ReviewVO> reviews = reviewMapper.listByOrder(order.getId());
        return buildOrders(List.of(order), viewerId, reviews).get(0);
    }

    private List<OrderVO> buildOrders(List<TradeOrder> orders, Long viewerId) {
        return buildOrders(orders, viewerId, null);
    }

    private List<OrderVO> buildOrders(List<TradeOrder> orders, Long viewerId, List<ReviewVO> sharedReviews) {
        if (orders == null || orders.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> itemIds = orders.stream().map(TradeOrder::getItemId).collect(Collectors.toSet());
        Set<Long> userIds = new HashSet<>();
        orders.forEach(order -> {
            userIds.add(order.getBuyerId());
            userIds.add(order.getSellerId());
        });

        Map<Long, Item> itemMap = itemMapper.findByIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));
        Map<Long, User> userMap = userMapper.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, String> coverMap = new HashMap<>();
        for (ItemImage image : itemImageMapper.findByItemIds(itemIds)) {
            coverMap.putIfAbsent(image.getItemId(), image.getUrl());
        }
        Map<Long, HandoverRecord> handoverMap = new HashMap<>();
        for (HandoverRecord record : handoverMapper.findByOrderIds(orders.stream().map(TradeOrder::getId).toList())) {
            handoverMap.put(record.getOrderId(), record);
        }

        List<OrderVO> result = new ArrayList<>();
        for (TradeOrder order : orders) {
            Item item = itemMap.get(order.getItemId());
            User buyer = userMap.get(order.getBuyerId());
            User seller = userMap.get(order.getSellerId());

            OrderVO vo = new OrderVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setStatus(order.getStatus());
            vo.setStatusText(TradeConst.orderStatusText(order.getStatus()));
            vo.setCreatedAt(order.getCreatedAt());
            vo.setAcceptedAt(order.getAcceptedAt());
            vo.setExpireAt(order.getExpireAt());
            vo.setBuyerConfirmedAt(order.getBuyerConfirmedAt());
            vo.setSellerConfirmedAt(order.getSellerConfirmedAt());
            vo.setCompletedAt(order.getCompletedAt());
            vo.setRequestRemark(order.getRequestRemark());
            vo.setCancelType(order.getCancelType());
            vo.setCancelTypeText(TradeConst.cancelTypeText(order.getCancelType()));
            vo.setCancelReason(order.getCancelReason());
            vo.setDisputeReason(order.getDisputeReason());
            vo.setMyRole(viewerId != null && order.getBuyerId().equals(viewerId) ? "BUYER" : "SELLER");

            if (item != null) {
                OrderVO.ItemBrief brief = new OrderVO.ItemBrief();
                brief.setId(item.getId());
                brief.setTitle(item.getTitle());
                brief.setCoverUrl(coverMap.get(item.getId()));
                brief.setPrice(item.getPrice());
                vo.setItem(brief);
            }
            vo.setBuyer(toBrief(buyer));
            vo.setSeller(toBrief(seller));

            HandoverRecord handover = handoverMap.get(order.getId());
            if (handover != null) {
                OrderVO.Handover hv = new OrderVO.Handover();
                hv.setHandoverTime(handover.getHandoverTime());
                hv.setArea(handover.getArea());
                hv.setNote(handover.getNote());
                hv.setBuyerConfirmedAt(handover.getBuyerConfirmedAt());
                hv.setSellerConfirmedAt(handover.getSellerConfirmedAt());
                hv.setUpdatedAt(handover.getUpdatedAt());
                vo.setHandover(hv);
            }

            List<ReviewVO> reviews = sharedReviews != null
                    ? sharedReviews
                    : reviewMapper.listByOrder(order.getId());
            vo.setReviews(reviews);
            vo.setMyReview(viewerId == null ? null
                    : reviews.stream().filter(review -> review.getAuthorId().equals(viewerId)).findFirst().orElse(null));
            vo.setActions(buildActions(order, viewerId, vo.getMyReview() != null));
            result.add(vo);
        }
        return result;
    }

    private OrderVO.UserBrief toBrief(User user) {
        if (user == null) {
            return null;
        }
        OrderVO.UserBrief brief = new OrderVO.UserBrief();
        brief.setId(user.getId());
        brief.setNickname(user.getNickname());
        brief.setAvatarUrl(user.getAvatarUrl());
        return brief;
    }

    private OrderVO.Actions buildActions(TradeOrder order, Long viewerId, boolean reviewed) {
        OrderVO.Actions actions = new OrderVO.Actions();
        if (viewerId == null) {
            return actions;
        }
        boolean buyer = order.getBuyerId().equals(viewerId);
        boolean seller = order.getSellerId().equals(viewerId);
        boolean participant = buyer || seller;
        if (!participant) {
            return actions;
        }
        String status = order.getStatus();
        boolean pending = TradeConst.ORDER_PENDING_SELLER.equals(status);
        boolean delivering = TradeConst.ORDER_DELIVERING.equals(status);
        boolean buyerConfirmed = TradeConst.ORDER_BUYER_CONFIRMED.equals(status);
        boolean sellerConfirmed = TradeConst.ORDER_SELLER_CONFIRMED.equals(status);

        actions.setCanAccept(seller && pending);
        actions.setCanReject(seller && pending);
        actions.setCanCancel((buyer && pending) || (delivering && order.getBuyerConfirmedAt() == null
                && order.getSellerConfirmedAt() == null));
        actions.setCanConfirm(delivering || (buyerConfirmed && seller) || (sellerConfirmed && buyer));
        actions.setCanDispute(delivering || buyerConfirmed || sellerConfirmed);
        actions.setCanEditHandover(delivering || buyerConfirmed || sellerConfirmed);
        actions.setCanReview(TradeConst.ORDER_COMPLETED.equals(status) && !reviewed
                && order.getCompletedAt() != null
                && order.getCompletedAt().plusDays(reviewWindowDays).isAfter(LocalDateTime.now()));
        return actions;
    }

    /* ------------------------------ 评价 ------------------------------ */

    @Transactional
    public ReviewVO createReview(Long orderId, Long authorId, OrderDtos.ReviewRequest request) {
        TradeOrder order = requireOrder(orderId);
        requireParticipant(order, authorId);
        if (!TradeConst.ORDER_COMPLETED.equals(order.getStatus())) {
            throw BizException.conflict("只有已完成的订单可以评价");
        }
        if (order.getCompletedAt() == null
                || order.getCompletedAt().plusDays(reviewWindowDays).isBefore(LocalDateTime.now())) {
            throw BizException.conflict("评价窗口已关闭（完成后 " + reviewWindowDays + " 天内可评价）");
        }
        if (reviewMapper.findByOrderAndAuthor(orderId, authorId) != null) {
            throw BizException.conflict("你已评价过该订单，不能重复评价");
        }
        Long targetId = order.getBuyerId().equals(authorId) ? order.getSellerId() : order.getBuyerId();
        Review review = new Review();
        review.setOrderId(orderId);
        review.setAuthorId(authorId);
        review.setTargetId(targetId);
        review.setRating(request.rating());
        review.setContent(TextUtils.trimTo(request.content(), 500));
        reviewMapper.insert(review);

        notificationService.send(targetId, TradeConst.NOTIFY_REVIEW, "收到新的交易评价",
                "对方对本次交易给出了 " + request.rating() + " 星评价。", "ORDER", orderId);
        auditService.log("ORDER_REVIEW", "ORDER", orderId, "用户提交交易评价", null, request.rating() + "星");
        List<ReviewVO> reviews = reviewMapper.listByOrder(orderId);
        return reviews.stream().filter(item -> item.getAuthorId().equals(authorId)).findFirst()
                .orElseThrow(() -> BizException.notFound("评价不存在"));
    }

    public List<ReviewVO> reviews(Long orderId, Long userId, boolean admin) {
        TradeOrder order = requireOrder(orderId);
        if (!admin) {
            requireParticipant(order, userId);
        }
        return reviewMapper.listByOrder(orderId);
    }

    /* ------------------------------ 超时处理 ------------------------------ */

    @Transactional
    public int handleTimeoutOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<TradeOrder> expired = orderMapper.findExpiredPending(now, 100);
        for (TradeOrder order : expired) {
            TradeOrder locked = orderMapper.findByIdForUpdate(order.getId());
            if (locked == null || !TradeConst.ORDER_PENDING_SELLER.equals(locked.getStatus())) {
                continue;
            }
            locked.setStatus(TradeConst.ORDER_CANCELLED);
            locked.setCancelType(TradeConst.CANCEL_TIMEOUT);
            locked.setCancelReason("超过 " + timeoutHours + " 小时未确认，系统自动取消");
            locked.setClosedAt(now);
            orderMapper.update(locked);
            Item item = itemMapper.findById(locked.getItemId());
            String title = item == null ? "商品" : item.getTitle();
            notificationService.send(locked.getBuyerId(), TradeConst.NOTIFY_ORDER, "待确认请求已超时取消",
                    "你对《" + title + "》的待确认请求超过 " + timeoutHours + " 小时未确认，已自动取消。",
                    "ORDER", locked.getId());
            notificationService.send(locked.getSellerId(), TradeConst.NOTIFY_ORDER, "待确认请求已超时取消",
                    "商品《" + title + "》的一条待确认请求超时未处理，已自动取消。", "ORDER", locked.getId());
            auditService.log("ORDER_TIMEOUT", "ORDER", locked.getId(), "待确认请求超时自动取消",
                    TradeConst.ORDER_PENDING_SELLER, TradeConst.ORDER_CANCELLED);
        }
        return expired.size();
    }

    /* ------------------------------ 工具 ------------------------------ */

    public TradeOrder requireOrder(Long orderId) {
        TradeOrder order = orderMapper.findById(orderId);
        if (order == null) {
            throw BizException.notFound("订单不存在");
        }
        return order;
    }

    private void requireParticipant(TradeOrder order, Long userId) {
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw BizException.forbidden("无权操作该订单");
        }
    }

    private String text(Object value) {
        return value == null ? "未设置" : String.valueOf(value);
    }
}
