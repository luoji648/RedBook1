package com.zhiyan.redbookbackend.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "调整已上架商品库存")
public class ProductStockUpdateDTO {

    @NotNull
    @Min(0)
    @Schema(description = "新的库存数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer stock;
}
