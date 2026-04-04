package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_share_link")
public class ShareLink {
    @TableId
    private String shortCode;
    private Long noteId;
    private Long creatorUserId;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
