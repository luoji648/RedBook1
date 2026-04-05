package com.zhiyan.redbookbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhiyan.redbookbackend.dto.LoginFormDTO;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.ChangePasswordDTO;
import com.zhiyan.redbookbackend.dto.req.ProfileUpdateDTO;
import com.zhiyan.redbookbackend.dto.req.UserInfoUpdateDTO;
import com.zhiyan.redbookbackend.entity.User;
import jakarta.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    /**
     * 统一登录：验证码登录见 {@code POST /user/login}；密码登录见 {@code POST /user/login/password}，内部均走此方法。
     */
    Result login(LoginFormDTO loginForm, HttpSession session);

    Result logout(String authorizationHeader);

    Result changePassword(ChangePasswordDTO dto);

    Result me();

    Result updateProfile(ProfileUpdateDTO dto, String authorization);

    Result myInfo();

    Result updateMyInfo(UserInfoUpdateDTO dto);

    Result sign();

    Result signCount();

    Result publicProfile(Long userId);
}
