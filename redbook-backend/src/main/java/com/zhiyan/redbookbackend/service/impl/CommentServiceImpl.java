package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.entity.Note;
import com.zhiyan.redbookbackend.entity.NoteComment;
import com.zhiyan.redbookbackend.mapper.NoteCommentMapper;
import com.zhiyan.redbookbackend.mapper.NoteMapper;
import com.zhiyan.redbookbackend.service.ICommentService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements ICommentService {

    @Resource
    private NoteCommentMapper noteCommentMapper;
    @Resource
    private NoteMapper noteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result add(Long noteId, Long parentId, Long replyToUserId, String content) {
        if (content == null || content.isBlank()) {
            return Result.fail("内容不能为空");
        }
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            return Result.fail("笔记不存在");
        }
        NoteComment c = new NoteComment();
        c.setNoteId(noteId);
        c.setUserId(UserHolder.getUser().getId());
        c.setParentId(parentId);
        c.setReplyToUserId(replyToUserId);
        c.setContent(content);
        c.setStatus(0);
        c.setCreateTime(LocalDateTime.now());
        noteCommentMapper.insert(c);
        note.setCommentCount((note.getCommentCount() == null ? 0 : note.getCommentCount()) + 1);
        noteMapper.updateById(note);
        return Result.ok(c.getId());
    }

    @Override
    public Result tree(Long noteId) {
        List<NoteComment> all = noteCommentMapper.selectList(
                Wrappers.<NoteComment>lambdaQuery()
                        .eq(NoteComment::getNoteId, noteId)
                        .eq(NoteComment::getStatus, 0)
                        .orderByAsc(NoteComment::getCreateTime));
        Map<Long, List<NoteComment>> byParent = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));
        List<Map<String, Object>> roots = new ArrayList<>();
        for (NoteComment c : byParent.getOrDefault(0L, List.of())) {
            roots.add(toNode(c, byParent));
        }
        return Result.ok(roots);
    }

    private Map<String, Object> toNode(NoteComment c, Map<Long, List<NoteComment>> byParent) {
        Map<String, Object> m = new HashMap<>();
        m.put("comment", c);
        List<Map<String, Object>> children = new ArrayList<>();
        for (NoteComment ch : byParent.getOrDefault(c.getId(), List.of())) {
            children.add(toNode(ch, byParent));
        }
        m.put("replies", children);
        return m;
    }
}
