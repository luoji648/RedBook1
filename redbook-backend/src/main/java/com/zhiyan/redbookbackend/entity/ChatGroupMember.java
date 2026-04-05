package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_chat_group_member")
public class ChatGroupMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long userId;
    /** 0 成员 1 群主 */
    private Integer role;
    /** 0 在群 1 已移出（会话保留，不可收发新消息） */
    private Integer memberStatus;
    private LocalDateTime createTime;
    /** 已读至该群消息 id */
    private Long lastReadMsgId;
}
