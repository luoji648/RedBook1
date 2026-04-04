package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.CommentAddDTO;
import com.zhiyan.redbookbackend.service.ICommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "评论")
@RestController
@RequestMapping("/note/comment")
@RequiredArgsConstructor
public class CommentController {

    private final ICommentService commentService;

    @Operation(summary = "发表评论")
    @PostMapping("/{noteId}")
    public Result add(@PathVariable Long noteId, @RequestBody CommentAddDTO dto) {
        return commentService.add(noteId, dto.getParentId(), dto.getReplyToUserId(), dto.getContent());
    }

    @Operation(summary = "评论树（公开）")
    @GetMapping("/tree/{noteId}")
    public Result tree(@PathVariable Long noteId) {
        return commentService.tree(noteId);
    }
}
