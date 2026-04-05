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
    /** 实付（分），从钱包扣减 */
    private Long payCent;
    /** 优惠券抵扣（分） */
    private Long discountCent;
    /** 使用的 tb_user_coupon.id */
    private Long userCouponId;
    /** 0 created 1 paid 2 cancelled 3 refunded */
    private Integer status;
    /** 支付成功时间（用于 1 小时退款窗口与卖家结算） */
    private LocalDateTime payTime;
    /** 0 未结算 1 实付已按卖家入账 */
    private Integer sellerSettled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
