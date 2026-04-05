package com.zhiyan.redbookbackend.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "钱包余额")
public class WalletBalanceVO {
    @Schema(description = "余额，单位：分")
    private Long balanceCent;
}
