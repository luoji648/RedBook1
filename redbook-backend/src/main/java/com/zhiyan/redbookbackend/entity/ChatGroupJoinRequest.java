package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_chat_group_join_request")
public class ChatGroupJoinRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long applicantId;
    /** 0 待处理 1 已同意 2 已拒绝 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
