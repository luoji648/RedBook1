package com.zhiyan.redbookbackend.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InteractionRow {
    private String type;
    private Long actorId;
    private Long noteId;
    private LocalDateTime eventTime;
}
