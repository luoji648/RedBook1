package com.zhiyan.redbookbackend.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "帖子/商品详情「立即支付」下单")
public class OrderCreateDirectDTO {

    @NotNull
    @Schema(description = "商品 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @NotNull
    @Min(1)
    @Schema(description = "购买数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Schema(description = "用户优惠券 tb_user_coupon.id，不传则不用券")
    private Long userCouponId;
}
