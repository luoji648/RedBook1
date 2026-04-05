package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_user_wallet")
public class UserWallet {
    @TableId(type = IdType.INPUT)
    private Long userId;
    private Long balanceCent;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
