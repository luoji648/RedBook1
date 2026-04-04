package com.zhiyan.redbookbackend.dto.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeAggMessage implements Serializable {
    private Long noteId;
    private long delta;
    private Long outboxId;
    private Long userId;
}
