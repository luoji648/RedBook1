package com.zhiyan.redbookbackend.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单详情（含是否可继续支付）")
public class OrderDetailVO {
    private Long id;
    private Integer status;
    private Long totalCent;
    private Long payCent;
    private Long discountCent;
    private Long userCouponId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @Schema(description = "待支付且未超时可 true")
    private Boolean payable;
    @Schema(description = "待支付订单的支付截止时间，超时后需重新下单")
    private LocalDateTime payDeadline;
    private List<OrderLineVO> items;
}
