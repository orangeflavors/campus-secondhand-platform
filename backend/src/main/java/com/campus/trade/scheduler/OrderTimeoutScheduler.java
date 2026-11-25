package com.campus.trade.scheduler;

import com.campus.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 待确认请求超时取消（BR-07 候选值 24 小时）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void cancelExpiredPendingOrders() {
        try {
            int cancelled = orderService.handleTimeoutOrders();
            if (cancelled > 0) {
                log.info("已自动取消 {} 条超时待确认请求", cancelled);
            }
        } catch (Exception ex) {
            log.error("处理超时待确认请求失败", ex);
        }
    }
}
