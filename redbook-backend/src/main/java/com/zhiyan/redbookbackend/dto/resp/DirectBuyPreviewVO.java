package com.zhiyan.redbookbackend.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "立即支付页预览")
public class DirectBuyPreviewVO {

    @Schema(description = "商品 id")
    private Long productId;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "封面")
    private String cover;
    @Schema(description = "单价（分）")
    private Long priceCent;
    @Schema(description = "购买数量")
    private Integer quantity;
    @Schema(description = "卖家用户 id，可能为空（历史数据）")
    private Long sellerId;
    @Schema(description = "当前用户是否已关注卖家")
    private Boolean followingSeller;
    @Schema(description = "未关注且存在卖家时的提示文案")
    private String followSellerCouponHint;
    @Schema(description = "已关注且尚未拥有该商品券时可 true，前端可展示领取入口")
    private Boolean claimableFollowCoupon;
    @Schema(description = "已领取且未过期的该商品券 id，可用于下单")
    private Long userCouponId;
    @Schema(description = "商品应付小计（分）")
    private Long totalCent;
    @Schema(description = "若使用 userCouponId 时的抵扣（分）")
    private Long discountCent;
    @Schema(description = "若使用 userCouponId 时的实付（分）")
    private Long payCent;
    @Schema(description = "钱包余额（分）")
    private Long walletBalanceCent;
}
