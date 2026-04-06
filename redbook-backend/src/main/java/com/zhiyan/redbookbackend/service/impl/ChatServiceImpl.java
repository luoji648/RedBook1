package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.entity.ChatMessage;
import com.zhiyan.redbookbackend.entity.ChatThread;
import com.zhiyan.redbookbackend.entity.User;
import com.zhiyan.redbookbackend.mapper.ChatMessageMapper;
import com.zhiyan.redbookbackend.mapper.ChatThreadMapper;
import com.zhiyan.redbookbackend.service.IChatService;
import com.zhiyan.redbookbackend.service.IUserService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ChatServiceImpl implements IChatService {

    @Resource
    private ChatThreadMapper chatThreadMapper;
    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private IUserService userService;

    @Override
    public Result threads(long current, long size) {
        Long me = UserHolder.getUser().getId();
        Page<ChatThread> page = Page.of(current, size);
        var p = chatThreadMapper.selectPage(page,
                Wrappers.<ChatThread>lambdaQuery()
                        .nested(w -> w.eq(ChatThread::getUserLow, me).or().eq(ChatThread::getUserHigh, me))
                        .orderByDesc(ChatThread::getLastMsgTime));
        List<ChatThread> records = p.getRecords();
        Set<Long> peerIds = new LinkedHashSet<>();
        for (ChatThread t : records) {
            long peer = me.equals(t.getUserLow()) ? t.getUserHigh() : t.getUserLow();
            peerIds.add(peer);
        }
        Map<Long, User> userById = new HashMap<>();
        if (!peerIds.isEmpty()) {
            for (User u : userService.listByIds(peerIds)) {
                if (u != null && u.getId() != null) {
                    userById.put(u.getId(), u);
                }
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ChatThread t : records) {
            rows.add(threadToRow(t, me, userById));
        }
        return Result.ok(rows, p.getTotal());
    }

    private Map<String, Object> threadToRow(ChatThread t, Long me, Map<Long, User> userById) {
        long peerId = me.equals(t.getUserLow()) ? t.getUserHigh() : t.getUserLow();
        User peer = userById.get(peerId);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", t.getId());
        row.put("userLow", t.getUserLow());
        row.put("userHigh", t.getUserHigh());
        if (peer != null) {
            row.put("peerNickName", peer.getNickName());
            String icon = peer.getIcon();
            row.put("peerIcon", icon != null && !icon.isBlank() ? icon : null);
        } else {
            row.put("peerNickName", null);
            row.put("peerIcon", null);
        }
        row.put("lastMsgTime", t.getLastMsgTime());
        row.put("unreadCount", countUnreadInThread(t, me));
        return row;
    }

    private long countUnreadInThread(ChatThread t, Long me) {
        var q = Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getThreadId, t.getId())
                .ne(ChatMessage::getSenderId, me);
        Long readId = me.equals(t.getUserLow()) ? t.getUserLowReadMsgId() : t.getUserHighReadMsgId();
        if (readId != null) {
            q.gt(ChatMessage::getId, readId);
        }
        return chatMessageMapper.selectCount(q);
    }

    @Override
    public long sumPrivateUnreadForCurrentUser() {
        Long me = UserHolder.getUser().getId();
        List<ChatThread> list = chatThreadMapper.selectList(
                Wrappers.<ChatThread>lambdaQuery()
                        .nested(w -> w.eq(ChatThread::getUserLow, me).or().eq(ChatThread::getUserHigh, me)));
        long sum = 0;
        for (ChatThread t : list) {
            sum += countUnreadInThread(t, me);
        }
        return sum;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result markThreadRead(Long threadId) {
        Long me = UserHolder.getUser().getId();
        ChatThread t = chatThreadMapper.selectById(threadId);
        if (t == null || (!t.getUserLow().equals(me) && !t.getUserHigh().equals(me))) {
            return Result.fail("会话不存在");
        }
        ChatMessage last = chatMessageMapper.selectOne(
                Wrappers.<ChatMessage>lambdaQuery()
                        .eq(ChatMessage::getThreadId, threadId)
                        .orderByDesc(ChatMessage::getId)
                        .last("LIMIT 1"));
        if (last == null) {
            return Result.ok();
        }
        if (me.equals(t.getUserLow())) {
            t.setUserLowReadMsgId(last.getId());
        } else {
            t.setUserHighReadMsgId(last.getId());
        }
        chatThreadMapper.updateById(t);
        return Result.ok();
    }

    private void advanceThreadReadPointer(Long threadId, Long userId, Long msgId) {
        ChatThread t = chatThreadMapper.selectById(threadId);
        if (t == null) {
            return;
        }
        if (userId.equals(t.getUserLow())) {
            Long cur = t.getUserLowReadMsgId();
            if (cur == null || msgId > cur) {
                t.setUserLowReadMsgId(msgId);
                chatThreadMapper.updateById(t);
            }
        } else if (userId.equals(t.getUserHigh())) {
            Long cur = t.getUserHighReadMsgId();
            if (cur == null || msgId > cur) {
                t.setUserHighReadMsgId(msgId);
                chatThreadMapper.updateById(t);
            }
        }
    }

    @Override
    public Result messages(Long threadId, long current, long size) {
        Long me = UserHolder.getUser().getId();
        ChatThread t = chatThreadMapper.selectById(threadId);
        if (t == null || (!t.getUserLow().equals(me) && !t.getUserHigh().equals(me))) {
            return Result.fail("会话不存在");
        }
        Page<ChatMessage> page = Page.of(current, size);
        var p = chatMessageMapper.selectPage(page,
                Wrappers.<ChatMessage>lambdaQuery()
                        .eq(ChatMessage::getThreadId, threadId)
                        .orderByDesc(ChatMessage::getCreateTime));
        return Result.ok(p.getRecords(), p.getTotal());
    }

    private ChatThread getOrCreateThread(Long me, Long peerUserId) {
        long low = Math.min(me, peerUserId);
        long high = Math.max(me, peerUserId);
        ChatThread t = chatThreadMapper.selectOne(Wrappers.<ChatThread>lambdaQuery()
                .eq(ChatThread::getUserLow, low)
                .eq(ChatThread::getUserHigh, high));
        if (t == null) {
            t = new ChatThread();
            t.setUserLow(low);
            t.setUserHigh(high);
            t.setLastMsgTime(LocalDateTime.now());
            chatThreadMapper.insert(t);
        }
        return t;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result ensureThread(Long peerUserId) {
        Long me = UserHolder.getUser().getId();
        if (me.equals(peerUserId)) {
            return Result.fail("不能与自己会话");
        }
        ChatThread t = getOrCreateThread(me, peerUserId);
        return Result.ok(t.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result send(Long toUserId, String content) {
        if (content == null || content.isBlank()) {
            return Result.fail("内容不能为空");
        }
        Long me = UserHolder.getUser().getId();
        if (me.equals(toUserId)) {
            return Result.fail("不能给自己发私信");
        }
        ChatThread t = getOrCreateThread(me, toUserId);
        ChatMessage m = new ChatMessage();
        m.setThreadId(t.getId());
        m.setSenderId(me);
        m.setContent(content);
        m.setReadFlag(0);
        m.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(m);
        t.setLastMsgTime(LocalDateTime.now());
        chatThreadMapper.updateById(t);
        advanceThreadReadPointer(t.getId(), me, m.getId());
        return Result.ok(m.getId());
    }
}
