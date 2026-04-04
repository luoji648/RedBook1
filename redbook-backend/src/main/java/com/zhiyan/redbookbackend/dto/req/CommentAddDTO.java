package com.zhiyan.redbookbackend.dto.req;

import lombok.Data;

@Data
public class CommentAddDTO {
    private Long parentId;
    private Long replyToUserId;
    private String content;
}
