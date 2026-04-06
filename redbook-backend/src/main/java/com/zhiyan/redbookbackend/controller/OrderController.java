package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.OrderCreateDirectDTO;
import com.zhiyan.redbookbackend.dto.req.OrderCreateFromCartDTO;
import com.zhiyan.redbookbackend.dto.req.OrderPayDTO;
import com.zhiyan.redbookbackend.service.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @PostMapping("/create")
    public Result create(@RequestBody(required = false) OrderCreateFromCartDTO dto) {
        return orderService.createFromCart(dto == null ? new OrderCreateFromCartDTO() : dto);
    }

    @Operation(summary = "立即支付页预览（关注状态、可领券、试算实付与钱包余额）")
    @GetMapping("/direct-preview")
    public Result directPreview(@RequestParam Long productId,
                                @RequestParam(defaultValue = "1") int quantity,
                                @RequestParam(required = false) Long fallbackSellerUserId) {
        return orderService.directBuyPreview(productId, quantity, fallbackSellerUserId);
    }

    @Operation(summary = "单商品立即支付下单（不走购物车）")
    @PostMapping("/create-direct")
    public Result createDirect(@Valid @RequestBody OrderCreateDirectDTO dto) {
        return orderService.createDirect(dto);
    }

    @GetMapping("/my")
    public Result my(@RequestParam(defaultValue = "1") long current,
                     @RequestParam(defaultValue = "20") long size) {
        return orderService.my(current, size);
    }

    @Operation(summary = "订单详情（明细商品行、待支付时是否可继续支付与截止时间）")
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable("id") Long id) {
        return orderService.detail(id);
    }

    @Operation(summary = "待支付订单继续支付（需提交模拟支付密码 123456；15 分钟内且库存与券仍有效）")
    @PostMapping("/pay/{id}")
    public Result pay(@PathVariable("id") Long id, @Valid @RequestBody OrderPayDTO dto) {
        return orderService.pay(id, dto.getPayPassword());
    }

    @PostMapping("/refund/{id}")
    public Result refund(@PathVariable("id") Long id) {
        return orderService.refund(id);
    }

    @Operation(summary = "删除订单记录（已支付/已取消/已退款），不可删除待支付订单")
    @DeleteMapping("/{id}")
    public Result deleteRecord(@PathVariable("id") Long id) {
        return orderService.deleteRecord(id);
    }

    /**
     * 支持两种路径：{@code /{id}/close} 为主（避免少数环境下路径解析问题）；{@code /close/{id}} 兼容旧调用。
     */
    @Operation(summary = "关闭订单（仅待支付）：取消订单；购物车商品仅在支付成功后才移除")
    @PostMapping({"/{id}/close", "/close/{id}"})
    public Result close(@PathVariable("id") Long id) {
        return orderService.close(id);
    }
}
