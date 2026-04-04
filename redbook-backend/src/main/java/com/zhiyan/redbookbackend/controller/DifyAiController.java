package com.zhiyan.redbookbackend.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhiyan.redbookbackend.dto.resp.StreamResponse;
import com.zhiyan.redbookbackend.entity.Note;
import com.zhiyan.redbookbackend.entity.NoteComment;
import com.zhiyan.redbookbackend.mapper.NoteCommentMapper;
import com.zhiyan.redbookbackend.mapper.NoteMapper;
import com.zhiyan.redbookbackend.service.DifyService;
import com.zhiyan.redbookbackend.util.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "AI 助手（Dify）")
@RestController
@RequestMapping("/ai")
public class DifyAiController {

    @Resource
    private DifyService difyService;
    @Resource
    private NoteMapper noteMapper;
    @Resource
    private NoteCommentMapper noteCommentMapper;

    @Value("${dify.key.note-assistant}")
    private String noteAssistantKey;

    @Operation(summary = "流式对话（SSE），mode: SUMMARY_POST | SUMMARY_COMMENTS | QA",
            description = "返回 text/event-stream。Swagger/Knife4j 往往无法展示流式正文，请用 curl 或前端 EventSource 测试。"
                    + " Dify 需使用「对话/Chatflow/Agent」类应用及 /v1/chat-messages；纯工作流应用应改用 /v1/workflows/run。"
                    + " 前几帧常为 workflow_started 等，answer 可能为 null，正文在后续 message/agent_message 等事件中。")
    @GetMapping(value = "/note/{noteId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StreamResponse> stream(
            @PathVariable Long noteId,
            @RequestParam String mode,
            @RequestParam(required = false) String question) {
        Long uid = UserHolder.getUser().getId();
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            return Flux.error(new IllegalArgumentException("笔记不存在"));
        }
        String postContent = "标题:" + note.getTitle() + "\n正文:" + note.getContent();
        String commentsText = "";
        if ("SUMMARY_COMMENTS".equals(mode) || "QA".equals(mode)) {
            List<NoteComment> comments = noteCommentMapper.selectList(
                    Wrappers.<NoteComment>lambdaQuery()
                            .eq(NoteComment::getNoteId, noteId)
                            .eq(NoteComment::getStatus, 0)
                            .orderByAsc(NoteComment::getCreateTime));
            commentsText = comments.stream()
                    .map(c -> "用户" + c.getUserId() + ":" + c.getContent())
                    .collect(Collectors.joining("\n"));
        }
        StringBuilder ctx = new StringBuilder(postContent);
        if (!commentsText.isEmpty()) {
            ctx.append("\n评论区:\n").append(commentsText);
        }
        Map<String, String> inputs = new HashMap<>();
        inputs.put("mode", mode);
        // Dify 工作流常见变量名；与控制台「开始」节点输入字段名一致
        inputs.put("post_content", postContent);
        inputs.put("comments", commentsText);
        inputs.put("note_context", ctx.toString());
        String query = "QA".equals(mode) && question != null && !question.isBlank()
                ? question
                : switch (mode) {
            case "SUMMARY_POST" -> "请总结上述帖子内容";
            case "SUMMARY_COMMENTS" -> "请总结上述评论区内容";
            default -> "请根据帖子与评论回答用户问题";
        };
        return difyService.streamingMessage(query, uid, noteAssistantKey, inputs);
    }
}
