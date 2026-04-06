package com.zhiyan.redbookbackend.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InteractionRow {
    /** LIKE / COLLECT / COMMENT / SHARE / FOLLOW（接口 JSON 仍用字段名 type） */
    private String interactionType;
    private Long actorId;
    private Long noteId;
    private LocalDateTime eventTime;
}
