package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.entity.Cart;
import com.zhiyan.redbookbackend.entity.OrderItem;
import com.zhiyan.redbookbackend.entity.Product;
import com.zhiyan.redbookbackend.entity.ShopOrder;
import com.zhiyan.redbookbackend.mapper.CartMapper;
import com.zhiyan.redbookbackend.mapper.OrderItemMapper;
import com.zhiyan.redbookbackend.mapper.ShopOrderMapper;
import com.zhiyan.redbookbackend.service.IOrderService;
import com.zhiyan.redbookbackend.service.IProductService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements IOrderService {

    @Resource
    private CartMapper cartMapper;
    @Resource
    private ShopOrderMapper shopOrderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private IProductService productService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createFromCart() {
        Long uid = UserHolder.getUser().getId();
        List<Cart> lines = cartMapper.selectList(Wrappers.<Cart>lambdaQuery().eq(Cart::getUserId, uid));
        if (lines.isEmpty()) {
            return Result.fail("购物车为空");
        }
        long total = 0;
        for (Cart line : lines) {
            Product p = productService.getById(line.getProductId());
            if (p == null) {
                return Result.fail("商品不存在: " + line.getProductId());
            }
            if (p.getStock() == null || p.getStock() < line.getQuantity()) {
                return Result.fail("库存不足: " + p.getTitle());
            }
            total += p.getPriceCent() * line.getQuantity();
        }
        ShopOrder order = new ShopOrder();
        order.setUserId(uid);
        order.setTotalCent(total);
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        shopOrderMapper.insert(order);
        for (Cart line : lines) {
            Product p = productService.getById(line.getProductId());
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(p.getId());
            oi.setQuantity(line.getQuantity());
            oi.setPriceCent(p.getPriceCent());
            orderItemMapper.insert(oi);
            p.setStock(p.getStock() - line.getQuantity());
            p.setUpdateTime(LocalDateTime.now());
            productService.updateById(p);
        }
        cartMapper.delete(Wrappers.<Cart>lambdaQuery().eq(Cart::getUserId, uid));
        return Result.ok(order.getId());
    }

    @Override
    public Result my(long current, long size) {
        Long uid = UserHolder.getUser().getId();
        Page<ShopOrder> page = Page.of(current, size);
        var p = shopOrderMapper.selectPage(page,
                Wrappers.<ShopOrder>lambdaQuery().eq(ShopOrder::getUserId, uid).orderByDesc(ShopOrder::getCreateTime));
        return Result.ok(p.getRecords(), p.getTotal());
    }
}
