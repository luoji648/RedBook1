package com.zhiyan.redbookbackend.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "待支付订单确认扣款（模拟支付密码）")
public class OrderPayDTO {

    @NotBlank(message = "请输入支付密码")
    @Schema(description = "模拟支付密码，固定为 123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String payPassword;
}
