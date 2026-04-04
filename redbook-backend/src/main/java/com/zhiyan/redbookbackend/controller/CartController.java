package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.service.ICartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "购物车")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final ICartService cartService;

    @PostMapping("/add")
    public Result add(@RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity) {
        return cartService.add(productId, quantity);
    }

    @GetMapping("/my")
    public Result my() {
        return cartService.my();
    }

    @PutMapping("/{cartId}")
    public Result update(@PathVariable Long cartId, @RequestParam int quantity) {
        return cartService.updateQty(cartId, quantity);
    }

    @DeleteMapping("/{cartId}")
    public Result remove(@PathVariable Long cartId) {
        return cartService.remove(cartId);
    }
}
