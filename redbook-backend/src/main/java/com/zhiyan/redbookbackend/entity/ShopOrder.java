package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_order")
public class ShopOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long totalCent;
    /** 0 created 1 paid 2 cancelled */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
