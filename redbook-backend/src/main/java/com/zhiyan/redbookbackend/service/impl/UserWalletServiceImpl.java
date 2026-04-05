package com.zhiyan.redbookbackend.service.impl;

import com.zhiyan.redbookbackend.entity.UserWallet;
import com.zhiyan.redbookbackend.mapper.UserWalletMapper;
import com.zhiyan.redbookbackend.service.IUserWalletService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserWalletServiceImpl implements IUserWalletService {

    @Resource
    private UserWalletMapper userWalletMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureWallet(Long userId) {
        UserWallet w = userWalletMapper.selectById(userId);
        if (w != null) {
            return;
        }
        UserWallet n = new UserWallet();
        n.setUserId(userId);
        n.setBalanceCent(0L);
        n.setCreateTime(LocalDateTime.now());
        n.setUpdateTime(LocalDateTime.now());
        userWalletMapper.insert(n);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long getBalanceCent(Long userId) {
        ensureWallet(userId);
        UserWallet w = userWalletMapper.selectById(userId);
        return w != null && w.getBalanceCent() != null ? w.getBalanceCent() : 0L;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryDeduct(Long userId, long amountCent) {
        if (amountCent <= 0) {
            return true;
        }
        ensureWallet(userId);
        return userWalletMapper.deductBalance(userId, amountCent) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBalance(Long userId, long amountCent) {
        if (amountCent <= 0) {
            return;
        }
        // 由 Mapper 侧 INSERT ... ON DUPLICATE 一次完成，勿再先 ensureWallet 再 UPDATE
        int n = userWalletMapper.addBalance(userId, amountCent);
        // MySQL：INSERT 常为 1；ON DUPLICATE 更新时驱动可能返回 2，故不能用 == 1
        if (n < 1) {
            throw new IllegalStateException("钱包入账失败");
        }
    }
}
