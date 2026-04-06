package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.ProductSaveDTO;
import com.zhiyan.redbookbackend.dto.req.ProductStockUpdateDTO;
import com.zhiyan.redbookbackend.entity.Product;
import com.zhiyan.redbookbackend.mapper.ProductMapper;
import com.zhiyan.redbookbackend.service.IProductService;
import com.zhiyan.redbookbackend.util.UserHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    @Override
    public Result saveFromDto(ProductSaveDTO dto) {
        if (UserHolder.getUser() == null) {
            return Result.fail("请先登录");
        }
        Long uid = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        Product p;
        if (dto.getId() != null) {
            p = getById(dto.getId());
            if (p == null) {
                return Result.fail("商品不存在");
            }
            if (p.getSellerId() != null && !Objects.equals(p.getSellerId(), uid)) {
                return Result.fail("无权修改该商品");
            }
        } else {
            p = new Product();
            p.setCreateTime(now);
            p.setSellerId(uid);
        }
        p.setTitle(dto.getTitle().trim());
        p.setCover(dto.getCover() == null ? "" : dto.getCover());
        p.setPriceCent(dto.getPriceCent());
        p.setStock(dto.getStock());
        p.setStatus(dto.getStatus());
        p.setUpdateTime(now);
        saveOrUpdate(p);
        return Result.ok(p.getId());
    }

    @Override
    public Result updatePublishedStock(Long productId, ProductStockUpdateDTO dto) {
        if (UserHolder.getUser() == null) {
            return Result.fail("请先登录");
        }
        Long uid = UserHolder.getUser().getId();
        Product p = getById(productId);
        if (p == null) {
            return Result.fail("商品不存在");
        }
        if (p.getSellerId() == null || !Objects.equals(p.getSellerId(), uid)) {
            return Result.fail("无权修改该商品");
        }
        if (p.getStatus() == null || p.getStatus() != 1) {
            return Result.fail("仅可调整已上架商品的库存");
        }
        LocalDateTime now = LocalDateTime.now();
        lambdaUpdate()
                .eq(Product::getId, productId)
                .set(Product::getStock, dto.getStock())
                .set(Product::getUpdateTime, now)
                .update();
        return Result.ok(null);
    }
}
