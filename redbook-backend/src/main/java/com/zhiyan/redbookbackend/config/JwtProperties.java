package com.zhiyan.redbookbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /**
     * HS256 secret, at least 256 bits recommended.
     */
    private String secret = "redbook-jwt-secret-key-change-me-in-production-min-32-chars!!";
    private long expireSeconds = 36000L;
}
