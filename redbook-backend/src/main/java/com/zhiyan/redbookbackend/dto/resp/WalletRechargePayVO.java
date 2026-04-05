package com.zhiyan.redbookbackend.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模拟充值结果：payFormHtml 为空表示已直接入账")
public class WalletRechargePayVO {
    @Schema(description = "商户订单号")
    private String outTradeNo;
    @Schema(description = "真实支付宝场景下的表单 HTML；假接口为 null")
    private String payFormHtml;
}
