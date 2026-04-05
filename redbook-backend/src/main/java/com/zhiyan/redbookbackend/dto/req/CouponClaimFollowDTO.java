package com.zhiyan.redbookbackend.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "关注卖家后领取该商品专属优惠券（每用户每商品仅一张）")
public class CouponClaimFollowDTO {

    @NotNull
    @Schema(description = "商品 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    /**
     * 商品未绑定 seller_id 时（如历史数据），传笔记作者等推广者用户 id，与预览接口一致，用于判断是否已关注。
     */
    @Schema(description = "可选：商品无卖家字段时的推广者/笔记作者用户 id")
    private Long fallbackSellerUserId;
}
