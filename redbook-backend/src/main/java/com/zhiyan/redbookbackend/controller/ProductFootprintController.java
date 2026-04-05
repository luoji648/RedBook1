package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.service.IProductFootprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商品足迹")
@RestController
@RequestMapping("/product/footprint")
@RequiredArgsConstructor
public class ProductFootprintController {

    private final IProductFootprintService productFootprintService;

    @Operation(summary = "记录或更新浏览某商品的足迹")
    @PostMapping("/record")
    public Result record(@RequestParam Long productId) {
        return productFootprintService.record(productId);
    }

    @Operation(summary = "我的商品足迹列表（含商品标题、封面、价格）")
    @GetMapping("/my")
    public Result my(@RequestParam(defaultValue = "1") long current,
                     @RequestParam(defaultValue = "20") long size) {
        return productFootprintService.my(current, size);
    }
}
