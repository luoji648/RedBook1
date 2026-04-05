package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.CouponClaimFollowDTO;
import com.zhiyan.redbookbackend.service.IUserCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "优惠券")
@RestController
@RequestMapping("/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final IUserCouponService userCouponService;

    @Operation(summary = "我的可用优惠券（未使用且未过期）")
    @GetMapping("/my")
    public Result my(@RequestParam(defaultValue = "1") long current,
                     @RequestParam(defaultValue = "20") long size) {
        return userCouponService.my(current, size);
    }

    @Operation(summary = "关注卖家后领取该商品专属券（面值约为单价 10%，每商品限领一次）")
    @PostMapping("/claim-follow")
    public Result claimFollow(@Valid @RequestBody CouponClaimFollowDTO dto) {
        return userCouponService.claimFollowSeller(dto);
    }
}
