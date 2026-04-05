package com.zhiyan.redbookbackend.service;

public interface IUserWalletService {

    /** 不存在则创建余额为 0 的钱包 */
    void ensureWallet(Long userId);

    long getBalanceCent(Long userId);

    /**
     * 扣减余额，余额不足时返回 false（调用方应在事务内处理）
     */
    boolean tryDeduct(Long userId, long amountCent);

    void addBalance(Long userId, long amountCent);
}
