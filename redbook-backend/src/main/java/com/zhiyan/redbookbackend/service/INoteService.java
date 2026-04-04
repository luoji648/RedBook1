package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.NoteSaveDTO;

public interface INoteService {

    Result save(NoteSaveDTO dto);

    Result publish(Long noteId);

    Result delete(Long noteId);

    Result detail(Long noteId);

    Result myNotes(long current, long size);

    Result feed(Long maxTime, Integer offset, int size);

    Result recommend(long current, long size);

    Result related(Long noteId);
}
