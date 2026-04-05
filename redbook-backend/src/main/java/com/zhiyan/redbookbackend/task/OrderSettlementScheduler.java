package com.zhiyan.redbookbackend.task;

import com.zhiyan.redbookbackend.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSettlementScheduler {

    private final IOrderService orderService;

    /** 每分钟将「支付满 1 小时」的已支付订单实付结算到卖家钱包 */
    @Scheduled(fixedDelay = 60_000L)
    public void settleToSellers() {
        try {
            int n = orderService.settlePaidOrdersToSellers();
            if (n > 0) {
                log.info("已支付订单结算给卖家: {} 笔", n);
            }
        } catch (Exception e) {
            log.warn("订单结算任务异常", e);
        }
    }
}
