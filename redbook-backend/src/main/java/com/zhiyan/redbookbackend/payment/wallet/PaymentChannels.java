package com.zhiyan.redbookbackend.payment.wallet;

/**
 * 钱包充值支付渠道编码，策略实现类 {@link IWalletRechargePaymentStrategy#channelCode()} 与此保持一致。
 */
public final class PaymentChannels {

    public static final String ALIPAY = "ALIPAY";

    private PaymentChannels() {
    }
}
