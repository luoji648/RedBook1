package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.entity.ChatMessage;
import com.zhiyan.redbookbackend.entity.ChatThread;
import com.zhiyan.redbookbackend.mapper.ChatMessageMapper;
import com.zhiyan.redbookbackend.mapper.ChatThreadMapper;
import com.zhiyan.redbookbackend.service.IChatService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ChatServiceImpl implements IChatService {

    @Resource
    private ChatThreadMapper chatThreadMapper;
    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Override
    public Result threads(long current, long size) {
        Long me = UserHolder.getUser().getId();
        Page<ChatThread> page = Page.of(current, size);
        var p = chatThreadMapper.selectPage(page,
                Wrappers.<ChatThread>lambdaQuery()
                        .nested(w -> w.eq(ChatThread::getUserLow, me).or().eq(ChatThread::getUserHigh, me))
                        .orderByDesc(ChatThread::getLastMsgTime));
        return Result.ok(p.getRecords(), p.getTotal());
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
        long low = Math.min(me, toUserId);
        long high = Math.max(me, toUserId);
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
        ChatMessage m = new ChatMessage();
        m.setThreadId(t.getId());
        m.setSenderId(me);
        m.setContent(content);
        m.setReadFlag(0);
        m.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(m);
        t.setLastMsgTime(LocalDateTime.now());
        chatThreadMapper.updateById(t);
        return Result.ok(m.getId());
    }
}
