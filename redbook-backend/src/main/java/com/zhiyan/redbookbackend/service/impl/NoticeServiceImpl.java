package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.resp.InteractionRow;
import com.zhiyan.redbookbackend.entity.Note;
import com.zhiyan.redbookbackend.entity.NoticeRead;
import com.zhiyan.redbookbackend.entity.User;
import com.zhiyan.redbookbackend.mapper.NoteMapper;
import com.zhiyan.redbookbackend.mapper.NoticeMapper;
import com.zhiyan.redbookbackend.mapper.NoticeReadMapper;
import com.zhiyan.redbookbackend.mapper.UserMapper;
import com.zhiyan.redbookbackend.service.INoticeService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NoticeServiceImpl implements INoticeService {

    private static final String CAT_LIKE_COLLECT = "like_collect";
    private static final String CAT_FOLLOW = "follow";
    private static final String CAT_COMMENT = "comment";

    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private NoticeReadMapper noticeReadMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private NoteMapper noteMapper;

    @Override
    public Result interactionPage(long current, long size, String category) {
        if (UserHolder.getUser() == null) {
            return Result.fail("未登录");
        }
        long ownerId = UserHolder.getUser().getId();
        long offset = (current - 1) * size;
        String cat = category == null ? "all" : category.trim().toLowerCase();
        Long total;
        List<InteractionRow> rows;
        switch (cat) {
            case "like_collect" -> {
                total = noticeMapper.countLikeCollect(ownerId);
                if (total == null) {
                    total = 0L;
                }
                rows = noticeMapper.listLikeCollect(ownerId, offset, (int) size);
            }
            case "follow" -> {
                total = noticeMapper.countFollowNotice(ownerId);
                if (total == null) {
                    total = 0L;
                }
                rows = noticeMapper.listFollowNotice(ownerId, offset, (int) size);
            }
            case "comment" -> {
                total = noticeMapper.countCommentShare(ownerId);
                if (total == null) {
                    total = 0L;
                }
                rows = noticeMapper.listCommentShare(ownerId, offset, (int) size);
            }
            default -> {
                total = noticeMapper.countInteractions(ownerId);
                if (total == null) {
                    total = 0L;
                }
                rows = noticeMapper.listInteractions(ownerId, offset, (int) size);
            }
        }
        if (rows.isEmpty()) {
            return Result.ok(List.of(), total);
        }
        Set<Long> uids = new HashSet<>();
        Set<Long> nids = new HashSet<>();
        for (InteractionRow r : rows) {
            uids.add(r.getActorId());
            if (r.getNoteId() != null) {
                nids.add(r.getNoteId());
            }
        }
        Map<Long, User> userMap = uids.isEmpty() ? Map.of() : userMapper.selectBatchIds(uids).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Map<Long, Note> noteMap = nids.isEmpty() ? Map.of() : noteMapper.selectBatchIds(nids).stream()
                .collect(Collectors.toMap(Note::getId, n -> n, (a, b) -> a));

        List<Map<String, Object>> list = new ArrayList<>();
        for (InteractionRow r : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", r.getType());
            item.put("eventTime", r.getEventTime());
            item.put("noteId", r.getNoteId());
            User u = userMap.get(r.getActorId());
            if (u != null) {
                Map<String, Object> actor = new HashMap<>();
                actor.put("id", u.getId());
                actor.put("nickName", u.getNickName());
                actor.put("icon", u.getIcon());
                item.put("actor", actor);
            }
            Note n = noteMap.get(r.getNoteId());
            item.put("noteTitle", n != null ? n.getTitle() : "");
            list.add(item);
        }
        return Result.ok(list, total);
    }

    private LocalDateTime getLastReadTime(long userId, String category) {
        NoticeRead r = noticeReadMapper.selectOne(Wrappers.<NoticeRead>lambdaQuery()
                .eq(NoticeRead::getUserId, userId)
                .eq(NoticeRead::getCategory, category));
        return r == null ? null : r.getLastReadTime();
    }

    private long safeCount(Long n) {
        return n == null ? 0L : n;
    }

    @Override
    public Map<String, Long> getNoticeUnreadCounts() {
        long ownerId = UserHolder.getUser().getId();
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("likeCollect", safeCount(noticeMapper.countUnreadLikeCollect(ownerId, getLastReadTime(ownerId, CAT_LIKE_COLLECT))));
        m.put("follow", safeCount(noticeMapper.countUnreadFollowNotice(ownerId, getLastReadTime(ownerId, CAT_FOLLOW))));
        m.put("comment", safeCount(noticeMapper.countUnreadCommentShare(ownerId, getLastReadTime(ownerId, CAT_COMMENT))));
        return m;
    }

    private String normalizeNoticeCategory(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toLowerCase();
        return switch (s) {
            case "like_collect", "like-collect" -> CAT_LIKE_COLLECT;
            case "follow" -> CAT_FOLLOW;
            case "comment" -> CAT_COMMENT;
            default -> null;
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result markNoticeCategoryRead(String category) {
        if (UserHolder.getUser() == null) {
            return Result.fail("未登录");
        }
        String cat = normalizeNoticeCategory(category);
        if (cat == null) {
            return Result.fail("分类无效");
        }
        long me = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        NoticeRead existing = noticeReadMapper.selectOne(Wrappers.<NoticeRead>lambdaQuery()
                .eq(NoticeRead::getUserId, me)
                .eq(NoticeRead::getCategory, cat));
        if (existing == null) {
            NoticeRead nr = new NoticeRead();
            nr.setUserId(me);
            nr.setCategory(cat);
            nr.setLastReadTime(now);
            noticeReadMapper.insert(nr);
        } else {
            existing.setLastReadTime(now);
            noticeReadMapper.updateById(existing);
        }
        return Result.ok();
    }
}
