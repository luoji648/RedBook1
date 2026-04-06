package com.zhiyan.redbookbackend.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.ProductSaveDTO;
import com.zhiyan.redbookbackend.dto.req.ProductStockUpdateDTO;
import com.zhiyan.redbookbackend.entity.Product;
import com.zhiyan.redbookbackend.service.IProductService;
import com.zhiyan.redbookbackend.util.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商品")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;

    @Operation(summary = "我的商品", description = "当前登录卖家发布的商品分页列表（含上架/下架）")
    @GetMapping("/my")
    public Result myList(@RequestParam(defaultValue = "1") long current,
                         @RequestParam(defaultValue = "20") long size) {
        if (UserHolder.getUser() == null) {
            return Result.fail("请先登录");
        }
        Long uid = UserHolder.getUser().getId();
        Page<Product> page = Page.of(current, size);
        var p = productService.page(page, Wrappers.<Product>lambdaQuery()
                .eq(Product::getSellerId, uid)
                .orderByDesc(Product::getCreateTime));
        return Result.ok(p.getRecords(), p.getTotal());
    }

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") long current,
                       @RequestParam(defaultValue = "20") long size) {
        Page<Product> page = Page.of(current, size);
        var p = productService.page(page, Wrappers.<Product>lambdaQuery().eq(Product::getStatus, 1).orderByDesc(Product::getCreateTime));
        return Result.ok(p.getRecords(), p.getTotal());
    }

    @GetMapping("/{id}")
    public Result get(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        return Result.ok(product);
    }

    @Operation(summary = "保存商品", description = "新建或更新商品；封面请先通过 OSS 预签名上传得到 URL")
    @PostMapping("/save")
    public Result save(@Valid @RequestBody ProductSaveDTO dto) {
        return productService.saveFromDto(dto);
    }

    @Operation(summary = "调整已上架商品库存", description = "卖家将已上架商品（status=1）的库存更新为指定数量")
    @PutMapping("/{id}/stock")
    public Result updateStock(@PathVariable Long id, @Valid @RequestBody ProductStockUpdateDTO dto) {
        return productService.updatePublishedStock(id, dto);
    }

    /** 与 POST /product/save 相同，保留兼容旧路径 */
    @Operation(summary = "保存商品（兼容路径）")
    @PostMapping("/admin")
    public Result adminSave(@Valid @RequestBody ProductSaveDTO dto) {
        return productService.saveFromDto(dto);
    }
}
