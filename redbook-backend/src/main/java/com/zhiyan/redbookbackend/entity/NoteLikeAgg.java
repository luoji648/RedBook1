package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_note_like_agg")
public class NoteLikeAgg {
    @TableId
    private Long noteId;
    private Long likeCount;
    private LocalDateTime updateTime;
}
