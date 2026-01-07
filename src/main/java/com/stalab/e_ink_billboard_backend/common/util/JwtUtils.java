package com.stalab.e_ink_billboard_backend.common.util;




import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: Jwt工具类
 * @Version: 1.0
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.header}")
    private String header;

    /**
     * 生成 Token
     * @param userId 用户ID
     * @param role 用户角色 (ADMIN / VISITOR)
     * @return 加密后的 Token 字符串
     */
    public String createToken(Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId.toString()) // 设置主题(通常是ID或用户名)
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 签名算法
                .compact();
    }

    /**
     * 从 Token 中获取 UserID
     */
    public Long getUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    /**
     * 从 Token 中获取 Role
     */
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * 验证 Token 是否有效
     * @param token 客户端传来的 token
     * @return boolean
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("JWT 签名无效或格式错误: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT 已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("不支持的 JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT Claims 字符串为空: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 解析 Claims (内部方法)
     * 使用 try-catch 是为了在解析失败时由 validateToken 捕获，而不是直接抛出 500
     */
    private Claims getClaims(String token) {
        // 去掉可能的 Bearer 前缀
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 生成安全密钥
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
