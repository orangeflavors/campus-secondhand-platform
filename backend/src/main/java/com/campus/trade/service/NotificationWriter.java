package com.campus.trade.service;

import com.campus.trade.common.TradeConst;
import com.campus.trade.entity.Notification;
import com.campus.trade.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知落库：使用独立事务，保证通知失败不回滚核心业务（BR-17 / AC-10）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWriter {

    private final NotificationMapper notificationMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attempt(Notification notification) {
        notificationMapper.insert(notification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attemptFailed(Notification notification) {
        Notification failed = new Notification();
        failed.setUserId(notification.getUserId());
        failed.setType(notification.getType());
        failed.setTitle(notification.getTitle());
        failed.setContent(notification.getContent());
        failed.setRelatedType(notification.getRelatedType());
        failed.setRelatedId(notification.getRelatedId());
        failed.setReadFlag(0);
        failed.setSendStatus(TradeConst.NOTIFY_FAILED);
        failed.setRetryCount(1);
        notificationMapper.insert(failed);
    }
}
