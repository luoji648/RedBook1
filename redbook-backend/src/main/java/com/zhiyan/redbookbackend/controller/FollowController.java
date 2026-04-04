package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.service.IFollowService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "关注")
@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
public class FollowController {

    private final IFollowService followService;

    @PostMapping("/{userId}")
    public Result follow(@PathVariable Long userId) {
        return followService.follow(userId);
    }

    @DeleteMapping("/{userId}")
    public Result unfollow(@PathVariable Long userId) {
        return followService.unfollow(userId);
    }

    @GetMapping("/followers")
    public Result followers(@RequestParam(defaultValue = "1") long current,
                            @RequestParam(defaultValue = "20") long size) {
        return followService.followers(current, size);
    }

    @GetMapping("/following")
    public Result following(@RequestParam(defaultValue = "1") long current,
                            @RequestParam(defaultValue = "20") long size) {
        return followService.following(current, size);
    }
}
