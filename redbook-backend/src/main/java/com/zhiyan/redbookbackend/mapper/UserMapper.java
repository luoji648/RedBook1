package com.zhiyan.redbookbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyan.redbookbackend.entity.User;

/**
 * <p>
 *  Mapper 接口
 * </p>
 */

public interface UserMapper extends BaseMapper<User> {

    User getByPhone(String phone);
}
