package com.zhiyan.redbookbackend.payment.wallet;

import com.zhiyan.redbookbackend.entity.WalletRecharge;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 单次钱包充值创建支付请求的上下文，供模板方法与具体渠道读写。
 */
@Getter
@RequiredArgsConstructor
public class WalletRechargePaymentContext {

    private final long userId;
    private final long amountCent;

    @Setter
    private String outTradeNo;

    @Setter
    private WalletRecharge pendingRecharge;
}
