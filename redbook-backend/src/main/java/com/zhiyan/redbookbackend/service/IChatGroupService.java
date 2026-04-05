package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.GroupCreateDTO;
import com.zhiyan.redbookbackend.dto.req.GroupUpdateDTO;

public interface IChatGroupService {

    Result create(GroupCreateDTO dto);

    /** 某用户作为群主公开的群列表（主页展示） */
    Result listByOwner(Long ownerUserId);

    /** 当前用户加入的群（消息列表） */
    Result myGroups();

    Result join(Long groupId);

    Result handleJoinRequest(Long requestId, boolean approve);

    Result messages(Long groupId, long current, long size);

    Result sendGroupMessage(Long groupId, String content);

    /** 群基础信息（成员可见） */
    Result groupMeta(Long groupId);

    /** 当前用户所加全部群的未读条数（他人消息且未读游标之后）。 */
    long sumGroupUnreadForCurrentUser();

    /** 进入群聊后标记已读到最新消息 */
    Result markGroupRead(Long groupId);

    /** 群详情与成员列表（在群成员可见，群主排首位） */
    Result groupDetail(Long groupId);

    /** 群主将成员移出群 */
    Result kickMember(Long groupId, Long targetUserId);

    /** 群主更新群名称、群头像 */
    Result updateGroupProfile(Long groupId, GroupUpdateDTO dto);
}
