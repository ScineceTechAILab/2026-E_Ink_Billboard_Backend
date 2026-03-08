package com.stalab.e_ink_billboard_backend.service.wx;

import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 * 负责生成和验证推送权限的验证码
 */
@Slf4j
@Service
public class VerificationService {

    private final StringRedisTemplate redisTemplate;

    // 验证码前缀
    private static final String PUSH_VERIFY_CODE_PREFIX = "push:verify:code:";
    // 验证码有效期（分钟）
    private static final long CODE_EXPIRE_MINUTES = 30;

    public VerificationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成验证码并存储
     * @return 6位数字验证码
     */
    public String generateCode() {
        // 生成6位数字验证码
        String code = RandomUtil.randomNumbers(6);

        String key = PUSH_VERIFY_CODE_PREFIX + code;
        redisTemplate.opsForValue().set(key, "valid", CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("生成验证码: {}", code);
        return code;
    }

    /**
     * 验证并消耗验证码
     * @param code 用户输入的验证码
     * @return true=验证通过, false=验证失败或已过期
     */
    public boolean validateAndConsumeCode(String code) {
        if (code == null || code.length() != 6) {
            return false;
        }

        String key = PUSH_VERIFY_CODE_PREFIX + code;
        Boolean exists = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(exists)) {
            // 验证通过，删除验证码（一次性使用）
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }
}
