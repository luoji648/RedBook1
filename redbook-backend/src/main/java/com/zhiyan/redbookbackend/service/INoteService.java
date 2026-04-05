package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.NoteSaveDTO;
import com.zhiyan.redbookbackend.entity.Note;

import java.util.Map;

public interface INoteService {

    Result save(NoteSaveDTO dto);

    Result publish(Long noteId);

    Result delete(Long noteId);

    Result detail(Long noteId);

    Result myNotes(long current, long size);

    Result feed(Long maxTime, Integer offset, int size);

    Result recommend(long current, long size);

    Result related(Long noteId);

    Result userNotes(long userId, long current, long size);

    /** 与详情页一致的可见性规则（含未登录访客） */
    boolean canViewerAccessNote(Note note, Long viewerId);

    /** 信息流/卡片用 VO（含 note、author、media 等） */
    Map<String, Object> noteCardVo(Note note, Long viewerId);
}
