package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.dto.Result;

public interface IFollowService {
    Result follow(Long followeeId);

    Result unfollow(Long followeeId);

    Result followers(long current, long size);

    Result following(long current, long size);
}
