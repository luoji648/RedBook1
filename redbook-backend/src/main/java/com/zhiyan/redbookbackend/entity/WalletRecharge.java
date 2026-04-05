package com.zhiyan.redbookbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_wallet_recharge")
public class WalletRecharge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 商户侧订单号 */
    private String outTradeNo;
    private Long amountCent;
    private String alipayTradeNo;
    /** 0 待支付 1 成功 2 关闭 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
}
