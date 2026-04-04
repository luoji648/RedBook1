package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_like_outbox")
public class LikeOutbox {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long noteId;
    /** 1 like 2 unlike */
    private Integer eventType;
    private String payload;
    private Integer published;
    private LocalDateTime createdTime;
    private LocalDateTime publishTime;
}
