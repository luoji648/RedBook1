package com.zhiyan.redbookbackend.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "发布/保存笔记")
public class NoteSaveDTO {
    @Schema(description = "更新时传笔记 id")
    private Long id;
    private String title;
    private String content;
    @Schema(description = "0 图文 1 视频")
    private Integer type;
    @Schema(description = "0 公开 1 仅关注 2 私密")
    private Integer visibility;
    private List<String> mediaUrls;
    private List<Long> productIds;
    @Schema(description = "true 直接发布，false 草稿")
    private Boolean publish;
}
