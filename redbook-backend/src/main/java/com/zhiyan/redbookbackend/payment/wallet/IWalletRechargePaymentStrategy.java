package com.zhiyan.redbookbackend.payment.wallet;

import com.zhiyan.redbookbackend.dto.Result;

import java.util.Map;

/**
 * 钱包充值支付策略：调用方只依赖此接口，具体渠道可切换。
 */
public interface IWalletRechargePaymentStrategy {

    String channelCode();

    Result createPay(long amountCent);

    /**
     * 渠道异步通知；不支持时返回 {@code failure}。
     */
    String handleNotify(Map<String, String> params);
}
