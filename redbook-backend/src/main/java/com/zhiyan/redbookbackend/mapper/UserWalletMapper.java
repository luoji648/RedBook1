package com.zhiyan.redbookbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyan.redbookbackend.entity.UserWallet;
import org.apache.ibatis.annotations.Param;

public interface UserWalletMapper extends BaseMapper<UserWallet> {

    int addBalance(@Param("userId") Long userId, @Param("amountCent") long amountCent);

    int deductBalance(@Param("userId") Long userId, @Param("amountCent") long amountCent);
}
