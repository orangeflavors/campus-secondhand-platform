package com.campus.trade.service;

import com.campus.trade.common.PageResult;
import com.campus.trade.common.PageUtils;
import com.campus.trade.common.TradeConst;
import com.campus.trade.entity.Notification;
import com.campus.trade.mapper.NotificationMapper;
import com.campus.trade.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 站内通知服务：核心事务提交后再投递通知，投递失败只记录失败状态（FR-032 / BR-17）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationWriter notificationWriter;
    private final NotificationMapper notificationMapper;

    private static final Map<String, String> TYPE_TEXT = Map.of(
            TradeConst.NOTIFY_ITEM, "商品",
            TradeConst.NOTIFY_ORDER, "订单",
            TradeConst.NOTIFY_REPORT, "举报",
            TradeConst.NOTIFY_REVIEW, "评价",
            TradeConst.NOTIFY_ACCOUNT, "账户",
            TradeConst.NOTIFY_SYSTEM, "系统"
    );

    public void send(Long userId, String type, String title, String content, String relatedType, Long relatedId) {
        if (userId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedType(relatedType);
        notification.setRelatedId(relatedId);
        notification.setReadFlag(0);
        notification.setSendStatus(TradeConst.NOTIFY_SENT);
        notification.setRetryCount(0);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deliver(notification);
                }
            });
        } else {
            deliver(notification);
        }
    }

    private void deliver(Notification notification) {
        try {
            notificationWriter.attempt(notification);
        } catch (Exception ex) {
            log.error("站内通知发送失败 userId={} title={}", notification.getUserId(), notification.getTitle(), ex);
            try {
                notificationWriter.attemptFailed(notification);
            } catch (Exception recordEx) {
                log.error("通知失败记录写入失败 userId={}", notification.getUserId(), recordEx);
            }
        }
    }

    public PageResult<NotificationVO> list(Long userId, Boolean unread, Integer page, Integer size) {
        int p = PageUtils.page(page);
        int s = PageUtils.size(size, 10);
        boolean onlyUnread = Boolean.TRUE.equals(unread);
        List<NotificationVO> records = notificationMapper.list(userId, onlyUnread, PageUtils.offset(p, s), s);
        records.forEach(vo -> vo.setTypeText(TYPE_TEXT.getOrDefault(vo.getType(), vo.getType())));
        long total = notificationMapper.count(userId, onlyUnread);
        return PageResult.of(records, total, p, s);
    }

    public long unreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        notificationMapper.markRead(id, userId, LocalDateTime.now());
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId, LocalDateTime.now());
    }
}
