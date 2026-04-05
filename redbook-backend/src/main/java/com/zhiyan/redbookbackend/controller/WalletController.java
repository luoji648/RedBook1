package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.WalletRechargeCreateDTO;
import com.zhiyan.redbookbackend.dto.resp.WalletBalanceVO;
import com.zhiyan.redbookbackend.service.IUserWalletService;
import com.zhiyan.redbookbackend.service.IWalletRechargeService;
import com.zhiyan.redbookbackend.util.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "钱包")
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final IUserWalletService userWalletService;
    private final IWalletRechargeService walletRechargeService;

    @Operation(summary = "当前用户钱包余额（分）")
    @GetMapping("/balance")
    public Result balance() {
        Long uid = UserHolder.getUser().getId();
        long cent = userWalletService.getBalanceCent(uid);
        return Result.ok(new WalletBalanceVO(cent));
    }

    @Operation(summary = "模拟支付宝充值（直接入账，不调真实支付宝）")
    @PostMapping("/recharge/alipay")
    public Result rechargeAlipay(@Valid @RequestBody WalletRechargeCreateDTO dto) {
        return walletRechargeService.createPagePay(dto.getAmountCent());
    }
}
