package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_chat_group")
public class ChatGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerId;
    private String name;
    /** 自定义群头像 URL，空则展示群主头像 */
    private String avatar;
    /** 0 无需验证 1 需群主验证 */
    private Integer joinMode;
    private LocalDateTime createTime;
    private LocalDateTime lastMsgTime;
}
