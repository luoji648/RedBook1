package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface ICommentService {
    Result add(Long noteId, Long parentId, Long replyToUserId, String content);

    Result tree(Long noteId);

    Result delete(Long commentId);
}
