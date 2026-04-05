package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface IChatService {
    Result threads(long current, long size);

    Result messages(Long threadId, long current, long size);

    Result send(Long toUserId, String content);

    /** 获取与对方的会话 id；若尚无会话则创建（无首条消息）。 */
    Result ensureThread(Long peerUserId);

    /** 当前用户全部私信的未读条数（对方发送且未读游标之后）。 */
    long sumPrivateUnreadForCurrentUser();

    /** 进入会话后标记已读到最新消息 */
    Result markThreadRead(Long threadId);
}
