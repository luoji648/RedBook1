package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.service.IChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "私信")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final IChatService chatService;

    @GetMapping("/threads")
    public Result threads(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "20") long size) {
        return chatService.threads(current, size);
    }

    @GetMapping("/{threadId}/messages")
    public Result messages(@PathVariable Long threadId,
                           @RequestParam(defaultValue = "1") long current,
                           @RequestParam(defaultValue = "50") long size) {
        return chatService.messages(threadId, current, size);
    }

    @PostMapping("/send")
    public Result send(@RequestParam Long toUserId, @RequestParam String content) {
        return chatService.send(toUserId, content);
    }
}
