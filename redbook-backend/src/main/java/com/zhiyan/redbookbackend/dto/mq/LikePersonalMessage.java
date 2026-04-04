package com.zhiyan.redbookbackend.dto.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikePersonalMessage implements Serializable {
    private Long userId;
    private Long noteId;
    /** 1 like, -1 unlike */
    private int action;
}
