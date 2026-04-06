package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.OrderCreateDirectDTO;
import com.zhiyan.redbookbackend.dto.req.OrderCreateFromCartDTO;

public interface IOrderService {
    Result createFromCart(OrderCreateFromCartDTO dto);

    /**
     * 帖子/商品详情「立即支付」预览（关注状态、可领券、实付试算）。
     * @param fallbackSellerUserId 商品无 seller_id 时，用笔记作者等作为「卖家」判断关注与领券提示
     */
    Result directBuyPreview(long productId, int quantity, Long fallbackSellerUserId);

    /** 单商品直购（不走购物车） */
    Result createDirect(OrderCreateDirectDTO dto);

    Result my(long current, long size);

    Result refund(Long orderId);

    /** 待支付订单：校验模拟支付密码后钱包扣款、占库存、核销券、置为已支付 */
    Result pay(Long orderId, String payPassword);

    /** 订单详情（含明细行与是否可继续支付） */
    Result detail(Long orderId);

    /** 取消创建时间超过 15 分钟仍未支付的订单（购物车未在下单时清空，无需再恢复） */
    int cancelExpiredPendingOrders();

    /** 支付成功满 1 小时且未退款的订单：将实付按商品行比例拆分并入账各卖家钱包 */
    int settlePaidOrdersToSellers();

    /** 用户主动关闭待支付订单（券仍为未使用；购物车商品仅在支付成功后才移除） */
    Result close(Long orderId);

    /** 删除订单记录（非待支付状态：已支付/已取消/已退款），物理删除订单及明细 */
    Result deleteRecord(Long orderId);
}
