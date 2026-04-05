package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_user_coupon")
public class UserCoupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 适用商品（关注卖家券等） */
    private Long productId;
    private String title;
    /** 满减金额（分） */
    private Long discountCent;
    /** 订单满额门槛（分） */
    private Long minOrderCent;
    /** 0 未使用 1 已使用 2 已过期 */
    private Integer status;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
