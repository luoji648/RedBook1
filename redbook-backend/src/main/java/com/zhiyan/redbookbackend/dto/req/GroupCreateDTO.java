package com.zhiyan.redbookbackend.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class GroupCreateDTO {
    @Schema(description = "群名称")
    private String name;
    /** 0 无需验证 1 需群主验证 */
    @Schema(description = "加群方式：0 无需验证，1 需群主验证")
    private Integer joinMode;
    @Schema(description = "群头像 URL（可选，OSS 等）；不传则创建后展示群主头像")
    private String avatar;
}
