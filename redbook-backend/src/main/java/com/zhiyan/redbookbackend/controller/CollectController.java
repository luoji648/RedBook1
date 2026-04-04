package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.service.ICollectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "收藏")
@RestController
@RequestMapping("/collect")
@RequiredArgsConstructor
public class CollectController {

    private final ICollectService collectService;

    @PostMapping("/{noteId}")
    public Result collect(@PathVariable Long noteId) {
        return collectService.collect(noteId);
    }

    @DeleteMapping("/{noteId}")
    public Result uncollect(@PathVariable Long noteId) {
        return collectService.uncollect(noteId);
    }

    @GetMapping("/my")
    public Result my(@RequestParam(defaultValue = "1") long current,
                     @RequestParam(defaultValue = "20") long size) {
        return collectService.my(current, size);
    }
}
