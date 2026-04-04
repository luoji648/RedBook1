package com.zhiyan.redbookbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyan.redbookbackend.entity.User;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 */

public interface UserMapper extends BaseMapper<User> {

    @Select("select * from tb_user where phone = #{phone}")
    User getByPhone(String phone);
}
