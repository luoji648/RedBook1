package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface ICollectService {
    Result collect(Long noteId);

    Result uncollect(Long noteId);

    Result my(long current, long size);

    /** 某用户收藏列表：本人或对方已公开时可查，笔记按访客可见性过滤 */
    Result userCollects(long profileUserId, long current, long size);
}
