package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.service.IChatGroupService;
import com.zhiyan.redbookbackend.service.IChatService;
import com.zhiyan.redbookbackend.service.INoticeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "私信")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final IChatService chatService;
    private final IChatGroupService chatGroupService;
    private final INoticeService noticeService;

    @GetMapping("/unread-total")
    public Result unreadTotal() {
        long chatPart = chatService.sumPrivateUnreadForCurrentUser()
                + chatGroupService.sumGroupUnreadForCurrentUser();
        var noticeCounts = noticeService.getNoticeUnreadCounts();
        long noticeSum = noticeCounts.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", chatPart + noticeSum);
        m.put("likeCollect", noticeCounts.getOrDefault("likeCollect", 0L));
        m.put("follow", noticeCounts.getOrDefault("follow", 0L));
        m.put("comment", noticeCounts.getOrDefault("comment", 0L));
        return Result.ok(m);
    }

    @GetMapping("/threads")
    public Result threads(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "20") long size) {
        return chatService.threads(current, size);
    }

    @PostMapping("/{threadId}/read")
    public Result markThreadRead(@PathVariable Long threadId) {
        return chatService.markThreadRead(threadId);
    }

    @GetMapping("/peer-thread")
    public Result peerThread(@RequestParam Long peerUserId) {
        return chatService.ensureThread(peerUserId);
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
