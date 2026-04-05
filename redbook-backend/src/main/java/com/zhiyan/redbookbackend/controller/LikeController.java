package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.service.ILikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "点赞")
@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class LikeController {

    private final ILikeService likeService;

    @PostMapping("/{noteId}")
    public Result like(@PathVariable Long noteId) {
        return likeService.like(noteId);
    }

    @DeleteMapping("/{noteId}")
    public Result unlike(@PathVariable Long noteId) {
        return likeService.unlike(noteId);
    }

    @GetMapping("/my")
    public Result my(@RequestParam(defaultValue = "1") long current,
                     @RequestParam(defaultValue = "20") long size) {
        return likeService.my(current, size);
    }

    @Operation(summary = "用户赞过列表（本人或对方已公开）")
    @GetMapping("/user/{userId}")
    public Result userLikes(@PathVariable Long userId,
                            @RequestParam(defaultValue = "1") long current,
                            @RequestParam(defaultValue = "20") long size) {
        return likeService.userLikes(userId, current, size);
    }
}
