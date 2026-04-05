package com.zhiyan.redbookbackend.payment.wallet;

import com.zhiyan.redbookbackend.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 从 IOC 收集全部 {@link IWalletRechargePaymentStrategy}，按 {@link IWalletRechargePaymentStrategy#channelCode()} 路由。
 * 新增渠道只需新增策略 Bean，无需修改本类。
 */
@Component
public class WalletRechargePaymentStrategyFactory {

    private final Map<String, IWalletRechargePaymentStrategy> strategies;

    public WalletRechargePaymentStrategyFactory(List<IWalletRechargePaymentStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(IWalletRechargePaymentStrategy::channelCode, Function.identity(), (a, b) -> {
                    throw new IllegalStateException("Duplicate payment channel: " + a.channelCode());
                }));
    }

    public IWalletRechargePaymentStrategy getRequired(String channelCode) {
        IWalletRechargePaymentStrategy s = strategies.get(channelCode);
        if (s == null) {
            throw new BizException("不支持的支付渠道: " + channelCode);
        }
        return s;
    }
}
