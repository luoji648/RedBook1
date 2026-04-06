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
@Schema(description = "我的订单列表行")
public class OrderListRowVO {
    private Long id;
    private Long totalCent;
    private Long payCent;
    private Long discountCent;
    private Integer status;
    private Integer sellerSettled;
    private LocalDateTime payTime;
    private LocalDateTime createTime;

    @Schema(description = "列表主标题（商品名或「首件名 等共 N 件」）")
    private String listTitle;

    @Schema(description = "订单内商品预览，顺序与明细行一致")
    private List<OrderListProductPreviewVO> products;
}
