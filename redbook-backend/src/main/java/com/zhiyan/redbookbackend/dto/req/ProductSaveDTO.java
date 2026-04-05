package com.zhiyan.redbookbackend.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "新建/更新商品")
public class ProductSaveDTO {

    @Schema(description = "更新时传商品 id")
    private Long id;

    @NotBlank
    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "封面图 URL，可为空")
    private String cover;

    @NotNull
    @Min(0)
    @Schema(description = "价格（分）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long priceCent;

    @NotNull
    @Min(0)
    @Schema(description = "库存", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer stock;

    /** 0 下架 1 上架 */
    @NotNull
    @Min(0)
    @Max(1)
    @Schema(description = "0 下架 1 上架", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
