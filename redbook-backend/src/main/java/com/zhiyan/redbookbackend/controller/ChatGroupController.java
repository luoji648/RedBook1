package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.GroupCreateDTO;
import com.zhiyan.redbookbackend.dto.req.GroupUpdateDTO;
import com.zhiyan.redbookbackend.service.IChatGroupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "群聊")
@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class ChatGroupController {

    private final IChatGroupService chatGroupService;

    @PostMapping("/create")
    public Result create(@RequestBody GroupCreateDTO dto) {
        return chatGroupService.create(dto);
    }

    /** 群主主页展示的群列表（无需登录） */
    @GetMapping("/by-owner/{ownerUserId}")
    public Result listByOwner(@PathVariable Long ownerUserId) {
        return chatGroupService.listByOwner(ownerUserId);
    }

    @GetMapping("/my")
    public Result myGroups() {
        return chatGroupService.myGroups();
    }

    @PostMapping("/{groupId}/join")
    public Result join(@PathVariable Long groupId) {
        return chatGroupService.join(groupId);
    }

    @PostMapping("/join-request/{requestId}/handle")
    public Result handleJoinRequest(@PathVariable Long requestId,
                                    @RequestParam boolean approve) {
        return chatGroupService.handleJoinRequest(requestId, approve);
    }

    @GetMapping("/{groupId}/meta")
    public Result meta(@PathVariable Long groupId) {
        return chatGroupService.groupMeta(groupId);
    }

    @GetMapping("/{groupId}/messages")
    public Result messages(@PathVariable Long groupId,
                           @RequestParam(defaultValue = "1") long current,
                           @RequestParam(defaultValue = "50") long size) {
        return chatGroupService.messages(groupId, current, size);
    }

    @PostMapping("/{groupId}/send")
    public Result send(@PathVariable Long groupId, @RequestParam String content) {
        return chatGroupService.sendGroupMessage(groupId, content);
    }

    @PostMapping("/{groupId}/read")
    public Result markRead(@PathVariable Long groupId) {
        return chatGroupService.markGroupRead(groupId);
    }

    @GetMapping("/{groupId}/detail")
    public Result detail(@PathVariable Long groupId) {
        return chatGroupService.groupDetail(groupId);
    }

    @PostMapping("/{groupId}/kick")
    public Result kick(@PathVariable Long groupId, @RequestParam Long targetUserId) {
        return chatGroupService.kickMember(groupId, targetUserId);
    }

    @PutMapping("/{groupId}/profile")
    public Result updateProfile(@PathVariable Long groupId, @RequestBody GroupUpdateDTO dto) {
        return chatGroupService.updateGroupProfile(groupId, dto);
    }
}
