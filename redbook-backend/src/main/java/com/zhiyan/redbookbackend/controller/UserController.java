package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.LoginFormDTO;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.ChangePasswordDTO;
import com.zhiyan.redbookbackend.dto.req.PasswordLoginDTO;
import com.zhiyan.redbookbackend.dto.req.ProfileUpdateDTO;
import com.zhiyan.redbookbackend.dto.req.UserInfoUpdateDTO;
import com.zhiyan.redbookbackend.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户与登录")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @Operation(summary = "发送短信验证码", description = "成功时 data 为 6 位验证码（模拟短信，便于联调）")
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    @Operation(summary = "验证码登录，返回 JWT（phone + code）")
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO form, HttpSession session) {
        return userService.login(form, session);
    }

    @Operation(summary = "密码登录，返回 JWT（phone + password）")
    @PostMapping("/login/password")
    public Result loginByPassword(@RequestBody PasswordLoginDTO dto, HttpSession session) {
        LoginFormDTO form = new LoginFormDTO();
        form.setPhone(dto.getPhone());
        form.setPassword(dto.getPassword());
        return userService.login(form, session);
    }

    @Operation(summary = "退出登录（JWT 加入黑名单）")
    @PostMapping("/logout")
    public Result logout(@RequestHeader(value = "authorization", required = false) String authorization) {
        return userService.logout(authorization);
    }

    @Operation(summary = "签到")
    @PostMapping("/sign")
    public Result sign() {
        return userService.sign();
    }

    @Operation(summary = "本月签到天数（当月 1 号至今天，含今天，统计已签到的自然日数）")
    @GetMapping("/sign/count")
    public Result signCount() {
        return userService.signCount();
    }

    @Operation(summary = "当前用户资料（账号+扩展）")
    @GetMapping("/me")
    public Result me() {
        return userService.me();
    }

    @Operation(summary = "公开用户主页资料（不含手机号）")
    @GetMapping("/public/{userId}")
    public Result publicProfile(@PathVariable("userId") Long userId) {
        return userService.publicProfile(userId);
    }

    @Operation(summary = "更新昵称与头像")
    @PutMapping("/profile")
    public Result profile(@RequestBody ProfileUpdateDTO dto,
                          @RequestHeader(value = "authorization", required = false) String authorization) {
        return userService.updateProfile(dto, authorization);
    }

    @Operation(summary = "扩展资料")
    @GetMapping("/info")
    public Result myInfo() {
        return userService.myInfo();
    }

    @Operation(summary = "更新扩展资料")
    @PutMapping("/info")
    public Result updateInfo(@RequestBody UserInfoUpdateDTO dto) {
        return userService.updateMyInfo(dto);
    }

    @Operation(summary = "修改密码（成功后旧 JWT 因 tokenVersion 失效）")
    @PutMapping("/password")
    public Result password(@RequestBody ChangePasswordDTO dto) {
        return userService.changePassword(dto);
    }
}
