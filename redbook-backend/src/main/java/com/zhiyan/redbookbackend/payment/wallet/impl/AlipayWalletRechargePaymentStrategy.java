package com.zhiyan.redbookbackend.payment.wallet.impl;

import com.zhiyan.redbookbackend.dto.resp.WalletRechargePayVO;
import com.zhiyan.redbookbackend.entity.WalletRecharge;
import com.zhiyan.redbookbackend.exception.BizException;
import com.zhiyan.redbookbackend.mapper.WalletRechargeMapper;
import com.zhiyan.redbookbackend.payment.wallet.AbstractWalletRechargePaymentStrategy;
import com.zhiyan.redbookbackend.payment.wallet.PaymentChannels;
import com.zhiyan.redbookbackend.payment.wallet.WalletRechargePaymentContext;
import com.zhiyan.redbookbackend.service.IUserWalletService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 支付宝充值 — 本地假实现：不调支付宝，直接增加钱包余额并记一条成功流水。
 */
@Slf4j
@Service
public class AlipayWalletRechargePaymentStrategy extends AbstractWalletRechargePaymentStrategy {

    private static final int ST_SUCCESS = 1;

    @Resource
    private WalletRechargeMapper walletRechargeMapper;
    @Resource
    private IUserWalletService userWalletService;

    @Override
    public String channelCode() {
        return PaymentChannels.ALIPAY;
    }

    @Override
    protected void validateForPay(WalletRechargePaymentContext ctx) {
        if (ctx.getAmountCent() <= 0) {
            throw new BizException("充值金额无效");
        }
    }

    @Override
    protected WalletRechargePayVO invokeChannelPayApi(WalletRechargePaymentContext ctx) {
        String outTradeNo = "WR-MOCK-" + ctx.getUserId() + "-" + UUID.randomUUID().toString().replace("-", "");

        WalletRecharge row = new WalletRecharge();
        row.setUserId(ctx.getUserId());
        row.setOutTradeNo(outTradeNo);
        row.setAmountCent(ctx.getAmountCent());
        row.setAlipayTradeNo("MOCK");
        row.setStatus(ST_SUCCESS);
        row.setCreateTime(LocalDateTime.now());
        row.setPayTime(LocalDateTime.now());
        walletRechargeMapper.insert(row);

        userWalletService.addBalance(ctx.getUserId(), ctx.getAmountCent());
        log.info("[wallet-recharge-mock] userId={} amountCent={} outTradeNo={}", ctx.getUserId(), ctx.getAmountCent(), outTradeNo);

        return new WalletRechargePayVO(outTradeNo, null);
    }

    @Override
    protected void afterChannelPaySuccess(WalletRechargePaymentContext ctx, WalletRechargePayVO vo) {
        // 已同步入账
    }

    @Override
    public String handleNotify(Map<String, String> params) {
        return "failure";
    }
}
