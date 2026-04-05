package com.zhiyan.redbookbackend.task;

import com.zhiyan.redbookbackend.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPayTimeoutScheduler {

    private final IOrderService orderService;

    /** 每分钟扫描一次超过 15 分钟未支付的待支付订单并取消 */
    @Scheduled(fixedDelay = 60_000L)
    public void cancelUnpaidOrders() {
        try {
            int n = orderService.cancelExpiredPendingOrders();
            if (n > 0) {
                log.info("超时未支付订单已取消: {} 笔", n);
            }
        } catch (Exception e) {
            log.warn("取消超时订单任务异常", e);
        }
    }
}
