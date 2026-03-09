package com.stalab.e_ink_billboard_backend.interceptor;

import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.mapper.UserMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 用户活跃度拦截器
 * 用于更新用户最后登录时间（最后活跃时间）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String USER_ACTIVE_KEY_PREFIX = "user:active:";
    private static final long ACTIVE_UPDATE_INTERVAL = 1; // 更新间隔（小时）

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取 Token
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            return true; // 没有 Token，直接放行（由 Controller 处理鉴权）
        }

        // 2. 校验 Token 是否有效
        // 注意：这里不需要抛出异常，如果 Token 无效，仅仅是不更新活跃时间而已
        if (jwtUtils.validateToken(token)) {
            try {
                Long userId = jwtUtils.getUserId(token);
                if (userId != null) {
                    updateLastActiveTime(userId);
                }
            } catch (Exception e) {
                log.warn("更新用户活跃时间失败: {}", e.getMessage());
            }
        }

        return true;
    }

    /**
     * 更新用户最后活跃时间（使用 Redis 防抖）
     */
    private void updateLastActiveTime(Long userId) {
        String key = USER_ACTIVE_KEY_PREFIX + userId;

        // 检查 Redis 中是否存在活跃标记
        Boolean hasKey = stringRedisTemplate.hasKey(key);

        if (Boolean.FALSE.equals(hasKey)) {
            // 如果不存在（说明距离上次更新已超过间隔时间），则更新数据库
            User user = new User();
            user.setId(userId);
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);

            // 设置 Redis 标记，过期时间为 1 小时
            stringRedisTemplate.opsForValue().set(key, "1", ACTIVE_UPDATE_INTERVAL, TimeUnit.HOURS);

            // log.debug("已更新用户 {} 的最后活跃时间", userId);
        }
    }
}
