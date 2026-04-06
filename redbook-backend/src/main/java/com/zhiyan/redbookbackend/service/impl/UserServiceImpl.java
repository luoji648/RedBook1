package com.zhiyan.redbookbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyan.redbookbackend.config.RedbookProperties;
import com.zhiyan.redbookbackend.dto.LoginFormDTO;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.UserDTO;
import com.zhiyan.redbookbackend.dto.req.ChangePasswordDTO;
import com.zhiyan.redbookbackend.dto.req.ProfileUpdateDTO;
import com.zhiyan.redbookbackend.dto.req.UserInfoUpdateDTO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhiyan.redbookbackend.entity.User;
import com.zhiyan.redbookbackend.entity.UserFollow;
import com.zhiyan.redbookbackend.entity.UserInfo;
import com.zhiyan.redbookbackend.mapper.UserFollowMapper;
import com.zhiyan.redbookbackend.mapper.UserMapper;
import com.zhiyan.redbookbackend.service.IUserInfoService;
import com.zhiyan.redbookbackend.service.IUserService;
import com.zhiyan.redbookbackend.service.JwtTokenService;
import com.zhiyan.redbookbackend.util.PasswordEncoder;
import com.zhiyan.redbookbackend.util.RedisConstants;
import com.zhiyan.redbookbackend.util.RegexUtils;
import com.zhiyan.redbookbackend.util.SystemConstants;
import com.zhiyan.redbookbackend.util.UserHolder;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private JwtTokenService jwtTokenService;
    @Resource
    private RedbookProperties redbookProperties;
    @Resource
    private IUserInfoService userInfoService;
    @Resource
    private UserFollowMapper userFollowMapper;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        String sendKey = RedisConstants.SEND_CODE_LIMIT_KEY + phone;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(sendKey))) {
            return Result.fail("发送过于频繁，请稍后再试");
        }
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String dayKey = RedisConstants.SMS_DAY_COUNT_KEY + phone + ":" + day;
        Long cnt = stringRedisTemplate.opsForValue().increment(dayKey);
        if (cnt != null && cnt == 1L) {
            stringRedisTemplate.expire(dayKey, 1, TimeUnit.DAYS);
        }
        if (cnt != null && cnt > redbookProperties.getSmsMaxPerDay()) {
            stringRedisTemplate.opsForValue().decrement(dayKey);
            return Result.fail("今日验证码次数已达上限");
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(sendKey, "1", 60, TimeUnit.SECONDS);
        log.debug("发送短信验证码成功，手机号:{} 验证码:{}", phone, code);
        return Result.ok(code);
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        User user;
        if (loginForm.getPassword() != null && !loginForm.getPassword().isBlank()) {
            user = userMapper.getByPhone(phone);
            if (user == null) {
                return Result.fail("手机号或密码错误");
            }
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                return Result.fail("请先使用验证码登录并设置密码");
            }
            try {
                if (!PasswordEncoder.matches(user.getPassword(), loginForm.getPassword())) {
                    return Result.fail("手机号或密码错误");
                }
            } catch (RuntimeException ex) {
                log.warn("password verify failed for phone={}", phone);
                return Result.fail("手机号或密码错误");
            }
        } else {
            String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
            String code = loginForm.getCode();
            if (cacheCode == null || !cacheCode.equals(code)) {
                return Result.fail("验证码错误或已过期");
            }
            stringRedisTemplate.delete(RedisConstants.LOGIN_CODE_KEY + phone);

            user = userMapper.getByPhone(phone);
            if (user == null) {
                user = createUserWithPhone(phone);
            }
        }
        if (user.getTokenVersion() == null) {
            user.setTokenVersion(0);
        }

        String jwt = jwtTokenService.createToken(user);
        Optional<Claims> claimsOpt = jwtTokenService.parse(jwt);
        if (claimsOpt.isEmpty()) {
            return Result.fail("登录失败");
        }
        String jti = claimsOpt.get().getId();

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? "" : fieldValue.toString()));

        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + jti, userMap);
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + jti, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(
                RedisConstants.USER_TOKEN_VERSION_KEY + user.getId(),
                String.valueOf(user.getTokenVersion()),
                RedisConstants.LOGIN_USER_TTL,
                TimeUnit.SECONDS
        );
        return Result.ok(jwt);
    }

    @Override
    public Result logout(String authorizationHeader) {
        Optional<Claims> opt = jwtTokenService.parse(authorizationHeader);
        if (opt.isEmpty()) {
            return Result.fail("未登录");
        }
        Claims claims = opt.get();
        String jti = claims.getId();
        long remainMs = claims.getExpiration().getTime() - System.currentTimeMillis();
        long ttlSec = Math.max(1, remainMs / 1000);
        stringRedisTemplate.opsForValue().set(RedisConstants.JWT_BLACKLIST_KEY + jti, "1", ttlSec, TimeUnit.SECONDS);
        stringRedisTemplate.delete(RedisConstants.LOGIN_USER_KEY + jti);
        return Result.ok();
    }

    @Override
    public Result changePassword(ChangePasswordDTO dto) {
        if (UserHolder.getUser() == null) {
            return Result.fail("未登录");
        }
        String newPassword = dto.getNewPassword();
        if (newPassword == null || newPassword.length() < 6) {
            return Result.fail("新密码至少6位");
        }
        User user = userMapper.selectById(UserHolder.getUser().getId());
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (dto.getOldPassword() == null || !PasswordEncoder.matches(user.getPassword(), dto.getOldPassword())) {
                return Result.fail("原密码错误");
            }
        }
        user.setPassword(PasswordEncoder.encode(newPassword));
        int tv = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        user.setTokenVersion(tv + 1);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        stringRedisTemplate.opsForValue().set(
                RedisConstants.USER_TOKEN_VERSION_KEY + user.getId(),
                String.valueOf(user.getTokenVersion()),
                RedisConstants.LOGIN_USER_TTL,
                TimeUnit.SECONDS
        );
        return Result.ok();
    }

    @Override
    public Result me() {
        if (UserHolder.getUser() == null) {
            return Result.fail("未登录");
        }
        Long id = UserHolder.getUser().getId();
        User user = userMapper.selectById(id);
        UserInfo info = userInfoService.getById(id);
        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("userInfo", info);
        return Result.ok(map);
    }

    @Override
    public Result updateProfile(ProfileUpdateDTO dto, String authorization) {
        if (UserHolder.getUser() == null) {
            return Result.fail("未登录");
        }
        Optional<Claims> opt = jwtTokenService.parse(authorization);
        if (opt.isEmpty()) {
            return Result.fail("未登录");
        }
        String jti = opt.get().getId();
        User user = userMapper.selectById(UserHolder.getUser().getId());
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (dto.getNickName() != null && !dto.getNickName().isBlank()) {
            user.setNickName(dto.getNickName());
        }
        if (dto.getIcon() != null) {
            user.setIcon(dto.getIcon());
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        refreshSessionRedis(jti, user);
        return Result.ok();
    }

    @Override
    public Result myInfo() {
        if (UserHolder.getUser() == null) {
            return Result.fail("未登录");
        }
        UserInfo info = userInfoService.getById(UserHolder.getUser().getId());
        return Result.ok(info);
    }

    @Override
    public Result updateMyInfo(UserInfoUpdateDTO dto) {
        if (UserHolder.getUser() == null) {
            return Result.fail("未登录");
        }
        Long uid = UserHolder.getUser().getId();
        UserInfo info = userInfoService.getById(uid);
        if (info == null) {
            return Result.fail("资料不存在");
        }
        if (dto.getCity() != null) {
            info.setCity(dto.getCity());
        }
        if (dto.getIntroduce() != null) {
            info.setIntroduce(dto.getIntroduce());
        }
        if (dto.getGender() != null) {
            info.setGender(dto.getGender());
        }
        if (dto.getBirthday() != null) {
            LocalDate birthdayMaxExclusive = LocalDate.of(2026, 1, 1);
            if (!dto.getBirthday().isBefore(birthdayMaxExclusive)) {
                return Result.fail("生日须为 2026-01-01 之前的日期");
            }
            info.setBirthday(dto.getBirthday());
        }
        if (dto.getCollectPublic() != null) {
            info.setCollectPublic(dto.getCollectPublic());
        }
        if (dto.getLikePublic() != null) {
            info.setLikePublic(dto.getLikePublic());
        }
        info.setUpdateTime(LocalDateTime.now());
        userInfoService.updateById(info);
        return Result.ok();
    }

    private void refreshSessionRedis(String jti, User user) {
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? "" : fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + jti, userMap);
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + jti, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);
    }

    @Override
    public Result sign() {
        Long userId = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyy/MM"));
        String key = RedisConstants.USER_SIGN_KEY + userId + keySuffix;
        int dayOfMonth = now.getDayOfMonth();
        int bitIndex = dayOfMonth - 1;
        Boolean already = stringRedisTemplate.opsForValue().getBit(key, bitIndex);
        if (Boolean.TRUE.equals(already)) {
            return Result.ok(Map.of("firstSignToday", false));
        }
        stringRedisTemplate.opsForValue().setBit(key, bitIndex, true);
        return Result.ok(Map.of("firstSignToday", true));
    }

    @Override
    public Result publicProfile(Long userId) {
        User user = getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        UserInfo info = userInfoService.getById(userId);
        Map<String, Object> pubUser = new HashMap<>();
        pubUser.put("id", user.getId());
        pubUser.put("nickName", user.getNickName());
        pubUser.put("icon", user.getIcon());
        Map<String, Object> map = new HashMap<>();
        map.put("user", pubUser);
        map.put("userInfo", info);
        Long viewer = UserHolder.getUser() != null ? UserHolder.getUser().getId() : null;
        boolean followedByMe = false;
        if (viewer != null && !viewer.equals(userId)) {
            Long c = userFollowMapper.selectCount(Wrappers.<UserFollow>lambdaQuery()
                    .eq(UserFollow::getFollowerId, viewer)
                    .eq(UserFollow::getFolloweeId, userId));
            followedByMe = c != null && c > 0;
        }
        map.put("followedByMe", followedByMe);
        map.put("isSelf", viewer != null && viewer.equals(userId));
        return Result.ok(map);
    }

    @Override
    public Result signCount() {
        Long userId = UserHolder.getUser().getId();
        LocalDate today = LocalDate.now();
        String keySuffix = today.format(DateTimeFormatter.ofPattern(":yyyy/MM"));
        String key = RedisConstants.USER_SIGN_KEY + userId + keySuffix;
        int throughDay = today.getDayOfMonth();
        // 本月 1 号～今天（含）共有几天已签到；与 sign() 同 key、位序（日-1）
        int cnt = 0;
        for (int d = 1; d <= throughDay; d++) {
            int bitIdx = d - 1;
            if (Boolean.TRUE.equals(stringRedisTemplate.opsForValue().getBit(key, bitIdx))) {
                cnt++;
            }
        }
        return Result.ok(cnt);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        user.setTokenVersion(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        UserInfo info = new UserInfo();
        info.setUserId(user.getId());
        info.setFans(0);
        info.setFollowee(0);
        info.setCredits(0);
        info.setLevel(0);
        info.setCreateTime(LocalDateTime.now());
        info.setUpdateTime(LocalDateTime.now());
        userInfoService.save(info);
        return user;
    }
}
