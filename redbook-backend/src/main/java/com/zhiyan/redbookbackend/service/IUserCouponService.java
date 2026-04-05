package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.CouponClaimFollowDTO;

public interface IUserCouponService {

    /**
     * 当前用户可用优惠券（未使用且未过期）分页列表。
     */
    Result my(long current, long size);

    /**
     * 关注卖家后领取该商品 10% 面值券（每用户每商品仅一次；券绑定 product_id）。
     */
    Result claimFollowSeller(CouponClaimFollowDTO dto);
}
