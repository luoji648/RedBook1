package com.zhiyan.redbookbackend.config;

import com.zhiyan.redbookbackend.interceptor.LoginInterceptor;
import com.zhiyan.redbookbackend.interceptor.RefreshInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class MvcConfig implements WebMvcConfigurer {

    /**
     * 仅业务 API 走登录校验与 JWT 刷新。默认 {@code /**} 会连静态资源（如 {@code /assets/*.js}）、{@code index.html}
     * 一并拦截，未登录时返回 401，表现为「JS 加载失败 / Network 里脚本 401」。
     */
    private static final String[] API_PATH_PATTERNS = {
            "/user/**",
            "/note/**",
            "/like/**",
            "/collect/**",
            "/follow/**",
            "/share/**",
            "/cart/**",
            "/order/**",
            "/chat/**",
            "/ai/**",
            "/oss/**",
    };

    private final LoginInterceptor loginInterceptor;
    private final RefreshInterceptor refreshInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns(API_PATH_PATTERNS)
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/note/detail/**",
                        "/note/recommend",
                        "/note/related/**",
                        "/note/comment/tree/**"
                )
                .order(1);

        registry.addInterceptor(refreshInterceptor)
                .addPathPatterns(API_PATH_PATTERNS)
                .order(0);
    }
}
