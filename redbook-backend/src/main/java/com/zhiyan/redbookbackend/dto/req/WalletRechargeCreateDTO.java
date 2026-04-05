package com.zhiyan.redbookbackend.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "钱包充值（分）")
public class WalletRechargeCreateDTO {

    @NotNull(message = "充值金额不能为空")
    @Min(value = 1, message = "充值金额至少 1 分")
    @Max(value = 500_000_00L, message = "单次充值金额过大")
    @Schema(description = "充值金额，单位：分", example = "10000")
    private Long amountCent;
}
