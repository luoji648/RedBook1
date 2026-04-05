package com.zhiyan.redbookbackend.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.ProductSaveDTO;
import com.zhiyan.redbookbackend.entity.Product;
import com.zhiyan.redbookbackend.service.IProductService;
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

    /** 与 POST /product/save 相同，保留兼容旧路径 */
    @Operation(summary = "保存商品（兼容路径）")
    @PostMapping("/admin")
    public Result adminSave(@Valid @RequestBody ProductSaveDTO dto) {
        return productService.saveFromDto(dto);
    }
}
