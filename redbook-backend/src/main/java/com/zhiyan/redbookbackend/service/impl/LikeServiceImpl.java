package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.redbookbackend.config.RedbookProperties;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.mq.LikePersonalMessage;
import com.zhiyan.redbookbackend.entity.Note;
import com.zhiyan.redbookbackend.entity.NoteLike;
import com.zhiyan.redbookbackend.mapper.NoteLikeMapper;
import com.zhiyan.redbookbackend.mapper.NoteMapper;
import com.zhiyan.redbookbackend.entity.UserInfo;
import com.zhiyan.redbookbackend.service.ILikeService;
import com.zhiyan.redbookbackend.service.INoteService;
import com.zhiyan.redbookbackend.service.IUserInfoService;
import com.zhiyan.redbookbackend.util.RabbitMqConstants;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LikeServiceImpl implements ILikeService {

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private RedbookProperties redbookProperties;
    @Resource
    private NoteMapper noteMapper;
    @Resource
    private NoteLikeMapper noteLikeMapper;
    @Resource
    private IUserInfoService userInfoService;
    @Resource
    private INoteService noteService;

    @Override
    public Result like(Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            return Result.fail("笔记不存在");
        }
        Long uid = UserHolder.getUser().getId();
        int sh = redbookProperties.getLikeUserShards();
        int shard = (int) Math.floorMod(uid, (long) sh);
        rabbitTemplate.convertAndSend(RabbitMqConstants.LIKE_USER_EXCHANGE, String.valueOf(shard),
                new LikePersonalMessage(uid, noteId, 1));
        return Result.ok();
    }

    @Override
    public Result unlike(Long noteId) {
        Long uid = UserHolder.getUser().getId();
        int sh = redbookProperties.getLikeUserShards();
        int shard = (int) Math.floorMod(uid, (long) sh);
        rabbitTemplate.convertAndSend(RabbitMqConstants.LIKE_USER_EXCHANGE, String.valueOf(shard),
                new LikePersonalMessage(uid, noteId, -1));
        return Result.ok();
    }

    @Override
    public Result my(long current, long size) {
        Long me = UserHolder.getUser().getId();
        Page<NoteLike> page = Page.of(current, size);
        var p = noteLikeMapper.selectPage(page,
                Wrappers.<NoteLike>lambdaQuery().eq(NoteLike::getUserId, me).orderByDesc(NoteLike::getCreateTime));
        List<Long> ids = p.getRecords().stream().map(NoteLike::getNoteId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Result.ok(List.of(), 0L);
        }
        List<Note> notes = noteMapper.selectList(Wrappers.<Note>lambdaQuery().in(Note::getId, ids));
        return Result.ok(notes, p.getTotal());
    }

    @Override
    public Result userLikes(long profileUserId, long current, long size) {
        Long viewer = UserHolder.getUser() != null ? UserHolder.getUser().getId() : null;
        if (viewer == null || !viewer.equals(profileUserId)) {
            UserInfo info = userInfoService.getById(profileUserId);
            if (info == null || !Boolean.TRUE.equals(info.getLikePublic())) {
                return Result.fail("该用户未公开赞过内容");
            }
        }
        Page<NoteLike> page = Page.of(current, size);
        var p = noteLikeMapper.selectPage(page,
                Wrappers.<NoteLike>lambdaQuery().eq(NoteLike::getUserId, profileUserId).orderByDesc(NoteLike::getCreateTime));
        List<Long> ids = p.getRecords().stream().map(NoteLike::getNoteId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Result.ok(List.of(), 0L);
        }
        List<Note> notes = noteMapper.selectList(Wrappers.<Note>lambdaQuery().in(Note::getId, ids));
        Map<Long, Note> byId = notes.stream().collect(Collectors.toMap(Note::getId, n -> n, (a, b) -> a));
        List<Map<String, Object>> vos = new ArrayList<>();
        for (Long nid : ids) {
            Note n = byId.get(nid);
            if (n == null || n.getStatus() == null || n.getStatus() != 1) {
                continue;
            }
            if (!noteService.canViewerAccessNote(n, viewer)) {
                continue;
            }
            vos.add(noteService.noteCardVo(n, viewer));
        }
        return Result.ok(vos, p.getTotal());
    }
}
