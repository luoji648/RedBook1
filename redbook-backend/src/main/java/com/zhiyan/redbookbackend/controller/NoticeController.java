package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.service.INoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "消息与通知")
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final INoticeService noticeService;

    @Operation(summary = "通知分页：category=all|like_collect|follow|comment（评论与转发）")
    @GetMapping("/interactions")
    public Result interactions(@RequestParam(defaultValue = "1") long current,
                               @RequestParam(defaultValue = "20") long size,
                               @RequestParam(defaultValue = "all") String category) {
        return noticeService.interactionPage(current, size, category);
    }
}
