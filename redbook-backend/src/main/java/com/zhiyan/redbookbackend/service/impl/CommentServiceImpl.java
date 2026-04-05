package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.entity.Note;
import com.zhiyan.redbookbackend.entity.NoteComment;
import com.zhiyan.redbookbackend.entity.User;
import com.zhiyan.redbookbackend.mapper.NoteCommentMapper;
import com.zhiyan.redbookbackend.mapper.NoteMapper;
import com.zhiyan.redbookbackend.mapper.UserMapper;
import com.zhiyan.redbookbackend.service.ICommentService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements ICommentService {

    @Resource
    private NoteCommentMapper noteCommentMapper;
    @Resource
    private NoteMapper noteMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ObjectMapper objectMapper;

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
        Long normalizedParentId = null;
        if (parentId != null) {
            NoteComment parent = noteCommentMapper.selectById(parentId);
            if (parent == null || (parent.getStatus() != null && parent.getStatus() != 0)) {
                return Result.fail("父评论不存在");
            }
            if (!noteId.equals(parent.getNoteId())) {
                return Result.fail("父评论不属于该笔记");
            }
            normalizedParentId = threadRootId(noteId, parent);
        }
        NoteComment c = new NoteComment();
        c.setNoteId(noteId);
        c.setUserId(UserHolder.getUser().getId());
        c.setParentId(normalizedParentId);
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
        Set<Long> visibleIds = all.stream().map(NoteComment::getId).collect(Collectors.toSet());
        Map<Long, NoteComment> byId = all.stream().collect(Collectors.toMap(NoteComment::getId, c -> c, (a, b) -> a));

        Map<Long, List<NoteComment>> byRoot = new HashMap<>();
        for (NoteComment c : all) {
            long rid = findRootId(c, byId, visibleIds);
            byRoot.computeIfAbsent(rid, k -> new ArrayList<>()).add(c);
        }

        Map<Long, User> userById = loadUsers(all);
        List<NoteComment> rootComments = all.stream()
                .filter(c -> effectiveParentId(c, visibleIds) == 0L)
                .sorted(Comparator.comparing(NoteComment::getCreateTime))
                .collect(Collectors.toList());

        List<Map<String, Object>> roots = new ArrayList<>();
        for (NoteComment root : rootComments) {
            List<NoteComment> thread = new ArrayList<>(byRoot.getOrDefault(root.getId(), List.of()));
            thread.sort(Comparator.comparing(NoteComment::getCreateTime));
            List<Map<String, Object>> replyNodes = new ArrayList<>();
            for (NoteComment rc : thread) {
                if (rc.getId().equals(root.getId())) {
                    continue;
                }
                replyNodes.add(toReplyNode(rc, userById));
            }
            roots.add(toRootNode(root, replyNodes, userById));
        }
        return Result.ok(roots);
    }

    /**
     * 仅二级评论：同一楼层内所有回复的 parentId 均指向该楼层根评论；树接口中回复列表扁平、无再嵌套。
     */
    private static long findRootId(NoteComment c, Map<Long, NoteComment> byId, Set<Long> visibleIds) {
        NoteComment x = c;
        while (true) {
            Long pid = x.getParentId();
            if (pid == null || pid == 0L) {
                return x.getId();
            }
            if (!visibleIds.contains(pid)) {
                return x.getId();
            }
            NoteComment p = byId.get(pid);
            if (p == null) {
                return x.getId();
            }
            x = p;
        }
    }

    /**
     * 写入时把 parentId 规范为「可见楼层根」的 id，保证数据库层面只有根 / 回复两级。
     */
    private Long threadRootId(Long noteId, NoteComment from) {
        NoteComment x = from;
        while (true) {
            Long pid = x.getParentId();
            if (pid == null || pid == 0L) {
                return x.getId();
            }
            NoteComment up = noteCommentMapper.selectById(pid);
            if (up == null || (up.getStatus() != null && up.getStatus() != 0) || !noteId.equals(up.getNoteId())) {
                return x.getId();
            }
            x = up;
        }
    }

    /**
     * 父评论已删除时，子评论挂到顶层，避免整串从树中消失。
     */
    private static long effectiveParentId(NoteComment c, Set<Long> visibleIds) {
        Long p = c.getParentId();
        long pid = p == null ? 0L : p;
        if (pid == 0L) {
            return 0L;
        }
        return visibleIds.contains(pid) ? pid : 0L;
    }

    private Map<Long, User> loadUsers(List<NoteComment> all) {
        Set<Long> ids = new HashSet<>();
        for (NoteComment c : all) {
            if (c.getUserId() != null) {
                ids.add(c.getUserId());
            }
            if (c.getReplyToUserId() != null) {
                ids.add(c.getReplyToUserId());
            }
        }
        Map<Long, User> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (User u : userMapper.selectBatchIds(ids)) {
            map.put(u.getId(), u);
        }
        return map;
    }

    private String displayNick(User u, Long userId) {
        if (u == null) {
            return "用户" + userId;
        }
        String n = u.getNickName();
        return (n != null && !n.isBlank()) ? n : ("用户" + u.getId());
    }

    /**
     * 将昵称、头像放进 comment Map（与实体字段同级），避免仅依赖节点外层字段时前端/代理漏传；序列化规则与全局 ObjectMapper 一致。
     */
    private Map<String, Object> commentToMap(NoteComment c, Map<Long, User> userById) {
        Map<String, Object> commentMap = objectMapper.convertValue(c, new TypeReference<Map<String, Object>>() {
        });
        User author = userById.get(c.getUserId());
        commentMap.put("nickName", displayNick(author, c.getUserId()));
        String icon = author != null && author.getIcon() != null ? author.getIcon() : "";
        commentMap.put("icon", icon);
        if (c.getReplyToUserId() != null) {
            User replyTo = userById.get(c.getReplyToUserId());
            commentMap.put("replyToNickName", displayNick(replyTo, c.getReplyToUserId()));
        }
        return commentMap;
    }

    private Map<String, Object> toRootNode(NoteComment root, List<Map<String, Object>> flatReplies,
                                           Map<Long, User> userById) {
        Map<String, Object> m = new HashMap<>();
        Map<String, Object> commentMap = commentToMap(root, userById);
        m.put("comment", commentMap);
        m.put("nickName", commentMap.get("nickName"));
        if (commentMap.containsKey("replyToNickName")) {
            m.put("replyToNickName", commentMap.get("replyToNickName"));
        }
        m.put("replies", flatReplies);
        return m;
    }

    private Map<String, Object> toReplyNode(NoteComment c, Map<Long, User> userById) {
        Map<String, Object> m = new HashMap<>();
        Map<String, Object> commentMap = commentToMap(c, userById);
        m.put("comment", commentMap);
        m.put("nickName", commentMap.get("nickName"));
        if (commentMap.containsKey("replyToNickName")) {
            m.put("replyToNickName", commentMap.get("replyToNickName"));
        }
        m.put("replies", List.of());
        return m;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result delete(Long commentId) {
        if (UserHolder.getUser() == null) {
            return Result.fail("请先登录");
        }
        NoteComment c = noteCommentMapper.selectById(commentId);
        if (c == null || c.getStatus() != null && c.getStatus() != 0) {
            return Result.fail("评论不存在");
        }
        if (!UserHolder.getUser().getId().equals(c.getUserId())) {
            return Result.fail("只能删除自己的评论");
        }
        c.setStatus(1);
        noteCommentMapper.updateById(c);
        Note note = noteMapper.selectById(c.getNoteId());
        if (note != null) {
            int cnt = note.getCommentCount() == null ? 0 : note.getCommentCount();
            note.setCommentCount(Math.max(0, cnt - 1));
            noteMapper.updateById(note);
        }
        return Result.ok();
    }
}
