package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.entity.Note;
import com.zhiyan.redbookbackend.entity.NoteCollect;
import com.zhiyan.redbookbackend.entity.UserInfo;
import com.zhiyan.redbookbackend.mapper.NoteCollectMapper;
import com.zhiyan.redbookbackend.mapper.NoteMapper;
import com.zhiyan.redbookbackend.service.ICollectService;
import com.zhiyan.redbookbackend.service.INoteService;
import com.zhiyan.redbookbackend.service.IUserInfoService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CollectServiceImpl implements ICollectService {

    @Resource
    private NoteCollectMapper noteCollectMapper;
    @Resource
    private NoteMapper noteMapper;
    @Resource
    private IUserInfoService userInfoService;
    @Resource
    private INoteService noteService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result collect(Long noteId) {
        Long me = UserHolder.getUser().getId();
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            return Result.fail("笔记不存在");
        }
        Long c = noteCollectMapper.selectCount(Wrappers.<NoteCollect>lambdaQuery()
                .eq(NoteCollect::getUserId, me)
                .eq(NoteCollect::getNoteId, noteId));
        if (c != null && c > 0) {
            return Result.ok();
        }
        NoteCollect nc = new NoteCollect();
        nc.setUserId(me);
        nc.setNoteId(noteId);
        nc.setCreateTime(LocalDateTime.now());
        noteCollectMapper.insert(nc);
        note.setCollectCount((note.getCollectCount() == null ? 0 : note.getCollectCount()) + 1);
        noteMapper.updateById(note);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result uncollect(Long noteId) {
        Long me = UserHolder.getUser().getId();
        int d = noteCollectMapper.delete(Wrappers.<NoteCollect>lambdaQuery()
                .eq(NoteCollect::getUserId, me)
                .eq(NoteCollect::getNoteId, noteId));
        if (d > 0) {
            Note note = noteMapper.selectById(noteId);
            if (note != null) {
                note.setCollectCount(Math.max(0, (note.getCollectCount() == null ? 0 : note.getCollectCount()) - 1));
                noteMapper.updateById(note);
            }
        }
        return Result.ok();
    }

    @Override
    public Result my(long current, long size) {
        Long me = UserHolder.getUser().getId();
        Page<NoteCollect> page = Page.of(current, size);
        var p = noteCollectMapper.selectPage(page,
                Wrappers.<NoteCollect>lambdaQuery().eq(NoteCollect::getUserId, me).orderByDesc(NoteCollect::getCreateTime));
        List<Long> ids = p.getRecords().stream().map(NoteCollect::getNoteId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Result.ok(List.of(), 0L);
        }
        List<Note> notes = noteMapper.selectList(Wrappers.<Note>lambdaQuery().in(Note::getId, ids));
        return Result.ok(notes, p.getTotal());
    }

    @Override
    public Result userCollects(long profileUserId, long current, long size) {
        Long viewer = UserHolder.getUser() != null ? UserHolder.getUser().getId() : null;
        if (viewer == null || !viewer.equals(profileUserId)) {
            UserInfo info = userInfoService.getById(profileUserId);
            if (info == null || !Boolean.TRUE.equals(info.getCollectPublic())) {
                return Result.fail("该用户未公开收藏内容");
            }
        }
        Page<NoteCollect> page = Page.of(current, size);
        var p = noteCollectMapper.selectPage(page,
                Wrappers.<NoteCollect>lambdaQuery().eq(NoteCollect::getUserId, profileUserId).orderByDesc(NoteCollect::getCreateTime));
        List<Long> ids = p.getRecords().stream().map(NoteCollect::getNoteId).collect(Collectors.toList());
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
