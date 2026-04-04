package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface ICollectService {
    Result collect(Long noteId);

    Result uncollect(Long noteId);

    Result my(long current, long size);
}
