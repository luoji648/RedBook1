package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface IChatService {
    Result threads(long current, long size);

    Result messages(Long threadId, long current, long size);

    Result send(Long toUserId, String content);
}
