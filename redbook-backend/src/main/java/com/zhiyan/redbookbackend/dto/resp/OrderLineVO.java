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
@Schema(description = "订单明细行")
public class OrderLineVO {
    private Long productId;
    private String title;
    private String cover;
    private Integer quantity;
    private Long priceCent;
}
