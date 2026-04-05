package com.zhiyan.redbookbackend.service.impl;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.resp.InteractionRow;
import com.zhiyan.redbookbackend.entity.Note;
import com.zhiyan.redbookbackend.entity.User;
import com.zhiyan.redbookbackend.mapper.NoteMapper;
import com.zhiyan.redbookbackend.mapper.NoticeMapper;
import com.zhiyan.redbookbackend.mapper.UserMapper;
import com.zhiyan.redbookbackend.service.INoticeService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NoticeServiceImpl implements INoticeService {

    @Resource
    private NoticeMapper noticeMapper;
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
}
