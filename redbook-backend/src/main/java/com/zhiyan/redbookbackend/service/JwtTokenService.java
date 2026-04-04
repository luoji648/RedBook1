package com.zhiyan.redbookbackend.service;

import com.zhiyan.redbookbackend.config.JwtProperties;
import com.zhiyan.redbookbackend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    public static final String CLAIM_TV = "tv";

    private final JwtProperties jwtProperties;

    private SecretKey key() {
        byte[] bytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(User user) {
        long now = System.currentTimeMillis();
        long exp = now + jwtProperties.getExpireSeconds() * 1000L;
        int tv = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        String jti = UUID.randomUUID().toString().replace("-", "");
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(jti)
                .claim(CLAIM_TV, tv)
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(key())
                .compact();
    }

    public Optional<Claims> parse(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }
        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7).trim()
                : authorizationHeader.trim();
        if (token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
