package com.zhiyan.redbookbackend.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "从购物车下单：可选使用一张未使用的优惠券")
public class OrderCreateFromCartDTO {

    @Schema(description = "用户优惠券 tb_user_coupon.id，不传则不用券")
    private Long userCouponId;
}
