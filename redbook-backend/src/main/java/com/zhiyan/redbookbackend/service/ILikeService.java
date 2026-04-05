package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface ILikeService {
    Result like(Long noteId);

    Result unlike(Long noteId);

    Result my(long current, long size);

    /** 某用户赞过列表：本人或对方已公开时可查，笔记按访客可见性过滤 */
    Result userLikes(long profileUserId, long current, long size);
}
