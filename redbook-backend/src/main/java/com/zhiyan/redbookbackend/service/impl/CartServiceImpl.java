package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.entity.Cart;
import com.zhiyan.redbookbackend.entity.Product;
import com.zhiyan.redbookbackend.mapper.CartMapper;
import com.zhiyan.redbookbackend.service.ICartService;
import com.zhiyan.redbookbackend.service.IProductService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CartServiceImpl implements ICartService {

    @Resource
    private CartMapper cartMapper;
    @Resource
    private IProductService productService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result add(Long productId, int quantity) {
        if (quantity <= 0) {
            return Result.fail("数量无效");
        }
        Product p = productService.getById(productId);
        if (p == null || p.getStatus() == null || p.getStatus() == 0) {
            return Result.fail("商品不存在或下架");
        }
        Long uid = UserHolder.getUser().getId();
        Cart exist = cartMapper.selectOne(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, uid)
                .eq(Cart::getProductId, productId));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + quantity);
            exist.setUpdateTime(LocalDateTime.now());
            cartMapper.updateById(exist);
            return Result.ok(exist.getId());
        }
        Cart c = new Cart();
        c.setUserId(uid);
        c.setProductId(productId);
        c.setQuantity(quantity);
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        cartMapper.insert(c);
        return Result.ok(c.getId());
    }

    @Override
    public Result my() {
        Long uid = UserHolder.getUser().getId();
        List<Cart> list = cartMapper.selectList(Wrappers.<Cart>lambdaQuery().eq(Cart::getUserId, uid));
        return Result.ok(list);
    }

    @Override
    public Result updateQty(Long cartId, int quantity) {
        if (quantity <= 0) {
            return Result.fail("数量无效");
        }
        Cart c = cartMapper.selectById(cartId);
        if (c == null || !c.getUserId().equals(UserHolder.getUser().getId())) {
            return Result.fail("记录不存在");
        }
        c.setQuantity(quantity);
        c.setUpdateTime(LocalDateTime.now());
        cartMapper.updateById(c);
        return Result.ok();
    }

    @Override
    public Result remove(Long cartId) {
        Cart c = cartMapper.selectById(cartId);
        if (c == null || !c.getUserId().equals(UserHolder.getUser().getId())) {
            return Result.fail("记录不存在");
        }
        cartMapper.deleteById(cartId);
        return Result.ok();
    }
}
