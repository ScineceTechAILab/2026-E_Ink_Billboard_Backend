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

        // 存入Redis，Key为验证码，Value为true（或者存生成时间等元数据）
        // 这里简化处理，只要Redis里有这个Key就认为有效
        // 注意：这种简单的Key设计在极高并发下可能有冲突，但对于6位数字和30分钟有效期，且用户量不大的场景是可以接受的。
        // 如果需要更严格，可以将Key设计为 code + userId，但那样用户在公众号获取验证码时需要先绑定，比较麻烦。
        // 当前需求是"关注公众号获取验证码"，通常是通用的验证码或者随机验证码。
        // 既然是随机生成的，我们假设它是随机的。

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
