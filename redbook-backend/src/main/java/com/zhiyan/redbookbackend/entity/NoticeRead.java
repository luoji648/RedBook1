package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_notice_read")
public class NoticeRead {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** like_collect | follow | comment */
    private String category;
    private LocalDateTime lastReadTime;
}
