package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.entity.Product;
import com.zhiyan.redbookbackend.entity.ProductFootprint;
import com.zhiyan.redbookbackend.mapper.ProductFootprintMapper;
import com.zhiyan.redbookbackend.service.IProductFootprintService;
import com.zhiyan.redbookbackend.service.IProductService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductFootprintServiceImpl implements IProductFootprintService {

    @Resource
    private ProductFootprintMapper productFootprintMapper;
    @Resource
    private IProductService productService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result record(Long productId) {
        if (productId == null) {
            return Result.fail("商品无效");
        }
        Product p = productService.getById(productId);
        if (p == null || p.getStatus() == null || p.getStatus() == 0) {
            return Result.fail("商品不存在或已下架");
        }
        Long uid = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        ProductFootprint exist = productFootprintMapper.selectOne(
                Wrappers.<ProductFootprint>lambdaQuery()
                        .eq(ProductFootprint::getUserId, uid)
                        .eq(ProductFootprint::getProductId, productId));
        if (exist != null) {
            exist.setViewedAt(now);
            productFootprintMapper.updateById(exist);
            return Result.ok(exist.getId());
        }
        ProductFootprint fp = new ProductFootprint();
        fp.setUserId(uid);
        fp.setProductId(productId);
        fp.setViewedAt(now);
        productFootprintMapper.insert(fp);
        return Result.ok(fp.getId());
    }

    @Override
    public Result my(long current, long size) {
        Long uid = UserHolder.getUser().getId();
        Page<ProductFootprint> page = Page.of(current, size);
        var p = productFootprintMapper.selectPage(page,
                Wrappers.<ProductFootprint>lambdaQuery()
                        .eq(ProductFootprint::getUserId, uid)
                        .orderByDesc(ProductFootprint::getViewedAt));
        List<ProductFootprint> rows = p.getRecords();
        if (rows.isEmpty()) {
            return Result.ok(new ArrayList<>(), p.getTotal());
        }
        List<Long> pids = rows.stream().map(ProductFootprint::getProductId).distinct().collect(Collectors.toList());
        List<Product> products = productService.listByIds(pids);
        Map<Long, Product> pmap = products.stream()
                .collect(Collectors.toMap(Product::getId, x -> x, (a, b) -> a));
        List<Map<String, Object>> list = new ArrayList<>();
        for (ProductFootprint fp : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", fp.getProductId());
            m.put("viewedAt", fp.getViewedAt());
            Product pr = pmap.get(fp.getProductId());
            if (pr != null) {
                m.put("title", pr.getTitle());
                m.put("cover", pr.getCover());
                m.put("priceCent", pr.getPriceCent());
            }
            list.add(m);
        }
        return Result.ok(list, p.getTotal());
    }
}
