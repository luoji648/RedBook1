package com.zhiyan.redbookbackend.service.impl;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.payment.wallet.PaymentChannels;
import com.zhiyan.redbookbackend.payment.wallet.WalletRechargePaymentStrategyFactory;
import com.zhiyan.redbookbackend.service.IWalletRechargeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletRechargeServiceImpl implements IWalletRechargeService {

    @Resource
    private WalletRechargePaymentStrategyFactory walletRechargePaymentStrategyFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createPagePay(long amountCent) {
        return walletRechargePaymentStrategyFactory.getRequired(PaymentChannels.ALIPAY).createPay(amountCent);
    }
}
