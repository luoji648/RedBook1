package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface ILikeService {
    Result like(Long noteId);

    Result unlike(Long noteId);

    Result my(long current, long size);
}
