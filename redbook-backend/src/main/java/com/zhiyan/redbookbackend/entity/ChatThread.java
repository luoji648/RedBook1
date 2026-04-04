package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_chat_thread")
public class ChatThread {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userLow;
    private Long userHigh;
    private LocalDateTime lastMsgTime;
}
