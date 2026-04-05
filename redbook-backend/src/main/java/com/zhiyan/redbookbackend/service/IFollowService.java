package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface IFollowService {
    Result follow(Long followeeId);

    Result unfollow(Long followeeId);

    Result followers(long current, long size);

    Result following(long current, long size);

    /** 指定用户的粉丝列表（公开，无需登录） */
    Result followersOfUser(Long userId, long current, long size);

    /** 指定用户的关注列表（公开，无需登录） */
    Result followingOfUser(Long userId, long current, long size);
}
