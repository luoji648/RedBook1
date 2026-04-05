package com.zhiyan.redbookbackend.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "我的优惠券（含关联商品信息）")
public class UserCouponVO {

    private Long id;
    private Long userId;
    private Long productId;
    private String title;
    private Long discountCent;
    private Long minOrderCent;
    private Integer status;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;

    @Schema(description = "关联商品标题，用于展示与跳转")
    private String productTitle;
    @Schema(description = "关联商品封面")
    private String productCover;
}
