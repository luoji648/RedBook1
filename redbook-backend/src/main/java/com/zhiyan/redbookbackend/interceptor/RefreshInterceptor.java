package com.zhiyan.redbookbackend.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.zhiyan.redbookbackend.dto.UserDTO;
import com.zhiyan.redbookbackend.service.JwtTokenService;
import com.zhiyan.redbookbackend.util.RedisConstants;
import com.zhiyan.redbookbackend.util.UserHolder;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RefreshInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenService jwtTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("authorization");
        if (StrUtil.isBlank(header)) {
            return true;
        }
        Optional<Claims> opt = jwtTokenService.parse(header);
        if (opt.isEmpty()) {
            // 无效 Token 按匿名处理；需登录的接口由 LoginInterceptor 再拦。避免公开接口（如 /note/recommend）因本地过期 JWT 恒 401
            UserHolder.removeUser();
            return true;
        }
        Claims claims = opt.get();
        String jti = claims.getId();
        if (StrUtil.isBlank(jti)) {
            UserHolder.removeUser();
            return true;
        }
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConstants.JWT_BLACKLIST_KEY + jti))) {
            UserHolder.removeUser();
            return true;
        }
        long userId = Long.parseLong(claims.getSubject());
        Integer jwtTv = claims.get(JwtTokenService.CLAIM_TV, Integer.class);

        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + jti);
        if (userMap.isEmpty()) {
            UserHolder.removeUser();
            return true;
        }

        Integer expectedTv = null;
        String tvRedis = stringRedisTemplate.opsForValue().get(RedisConstants.USER_TOKEN_VERSION_KEY + userId);
        if (tvRedis != null) {
            expectedTv = Integer.parseInt(tvRedis);
        } else {
            Object o = userMap.get("tokenVersion");
            if (o != null) {
                expectedTv = Integer.parseInt(o.toString());
            }
        }
        if (expectedTv != null && (jwtTv == null || !expectedTv.equals(jwtTv))) {
            UserHolder.removeUser();
            return true;
        }

        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        UserHolder.saveUser(userDTO);
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + jti, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
