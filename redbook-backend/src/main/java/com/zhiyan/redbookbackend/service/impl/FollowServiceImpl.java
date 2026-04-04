package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.entity.User;
import com.zhiyan.redbookbackend.entity.UserFollow;
import com.zhiyan.redbookbackend.entity.UserInfo;
import com.zhiyan.redbookbackend.mapper.UserFollowMapper;
import com.zhiyan.redbookbackend.mapper.UserMapper;
import com.zhiyan.redbookbackend.service.IFollowService;
import com.zhiyan.redbookbackend.service.IUserInfoService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl implements IFollowService {

    @Resource
    private UserFollowMapper userFollowMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private IUserInfoService userInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result follow(Long followeeId) {
        Long me = UserHolder.getUser().getId();
        if (me.equals(followeeId)) {
            return Result.fail("不能关注自己");
        }
        if (userMapper.selectById(followeeId) == null) {
            return Result.fail("用户不存在");
        }
        Long c = userFollowMapper.selectCount(Wrappers.<UserFollow>lambdaQuery()
                .eq(UserFollow::getFollowerId, me)
                .eq(UserFollow::getFolloweeId, followeeId));
        if (c != null && c > 0) {
            return Result.ok();
        }
        UserFollow f = new UserFollow();
        f.setFollowerId(me);
        f.setFolloweeId(followeeId);
        f.setCreateTime(LocalDateTime.now());
        userFollowMapper.insert(f);
        bumpFollowee(followeeId, 1);
        bumpFollower(me, 1);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result unfollow(Long followeeId) {
        Long me = UserHolder.getUser().getId();
        int d = userFollowMapper.delete(Wrappers.<UserFollow>lambdaQuery()
                .eq(UserFollow::getFollowerId, me)
                .eq(UserFollow::getFolloweeId, followeeId));
        if (d > 0) {
            bumpFollowee(followeeId, -1);
            bumpFollower(me, -1);
        }
        return Result.ok();
    }

    private void bumpFollowee(Long followeeId, int delta) {
        UserInfo info = userInfoService.getById(followeeId);
        if (info != null) {
            info.setFans(Math.max(0, (info.getFans() == null ? 0 : info.getFans()) + delta));
            info.setUpdateTime(LocalDateTime.now());
            userInfoService.updateById(info);
        }
    }

    private void bumpFollower(Long followerId, int delta) {
        UserInfo info = userInfoService.getById(followerId);
        if (info != null) {
            info.setFollowee(Math.max(0, (info.getFollowee() == null ? 0 : info.getFollowee()) + delta));
            info.setUpdateTime(LocalDateTime.now());
            userInfoService.updateById(info);
        }
    }

    @Override
    public Result followers(long current, long size) {
        Long me = UserHolder.getUser().getId();
        Page<UserFollow> page = Page.of(current, size);
        IPage<UserFollow> p = userFollowMapper.selectPage(page,
                Wrappers.<UserFollow>lambdaQuery().eq(UserFollow::getFolloweeId, me).orderByDesc(UserFollow::getCreateTime));
        List<Long> ids = p.getRecords().stream().map(UserFollow::getFollowerId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Result.ok(List.of(), 0L);
        }
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery().in(User::getId, ids));
        return Result.ok(users, p.getTotal());
    }

    @Override
    public Result following(long current, long size) {
        Long me = UserHolder.getUser().getId();
        Page<UserFollow> page = Page.of(current, size);
        IPage<UserFollow> p = userFollowMapper.selectPage(page,
                Wrappers.<UserFollow>lambdaQuery().eq(UserFollow::getFollowerId, me).orderByDesc(UserFollow::getCreateTime));
        List<Long> ids = p.getRecords().stream().map(UserFollow::getFolloweeId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Result.ok(List.of(), 0L);
        }
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery().in(User::getId, ids));
        return Result.ok(users, p.getTotal());
    }
}
