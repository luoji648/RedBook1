package com.zhiyan.redbookbackend.payment.wallet;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.resp.WalletRechargePayVO;
import com.zhiyan.redbookbackend.exception.BizException;
import com.zhiyan.redbookbackend.util.UserHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 模板方法：固定编排「校验 → 调渠道支付 → 后置处理」，并统一打日志；
 * 子类实现三个抽象步骤。
 */
@Slf4j
public abstract class AbstractWalletRechargePaymentStrategy implements IWalletRechargePaymentStrategy {

    @Override
    public final Result createPay(long amountCent) {
        Long uid = UserHolder.getUser().getId();
        log.info("[wallet-recharge] channel={} step=start userId={} amountCent={}", channelCode(), uid, amountCent);
        try {
            WalletRechargePaymentContext ctx = new WalletRechargePaymentContext(uid, amountCent);
            validateForPay(ctx);
            WalletRechargePayVO vo = invokeChannelPayApi(ctx);
            afterChannelPaySuccess(ctx, vo);
            log.info("[wallet-recharge] channel={} step=done outTradeNo={}", channelCode(), vo.getOutTradeNo());
            return Result.ok(vo);
        } catch (BizException e) {
            log.warn("[wallet-recharge] channel={} step=fail msg={}", channelCode(), e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 校验参数、渠道配置、用户态等。
     */
    protected abstract void validateForPay(WalletRechargePaymentContext ctx);

    /**
     * 调用渠道方支付 API（如拉起收银台），并组装返回给前端的 VO。
     */
    protected abstract WalletRechargePayVO invokeChannelPayApi(WalletRechargePaymentContext ctx);

    /**
     * 渠道返回成功后的本地后置处理（如补记账、发消息等）；无则空实现。
     */
    protected abstract void afterChannelPaySuccess(WalletRechargePaymentContext ctx, WalletRechargePayVO vo);

    @Override
    public String handleNotify(Map<String, String> params) {
        return "failure";
    }
}
