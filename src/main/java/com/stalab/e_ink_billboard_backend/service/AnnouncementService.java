package com.stalab.e_ink_billboard_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stalab.e_ink_billboard_backend.model.vo.AnnouncementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 公告服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private static final String ANNOUNCEMENT_KEY = "dashboard:announcement";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 获取公告
     */
    public AnnouncementVO getAnnouncement() {
        try {
            Object data = redisTemplate.opsForValue().get(ANNOUNCEMENT_KEY);
            if (data != null) {
                return objectMapper.convertValue(data, AnnouncementVO.class);
            }
        } catch (Exception e) {
            log.error("获取公告失败", e);
        }
        return null;
    }

    /**
     * 保存公告
     */
    public void saveAnnouncement(String content) {
        try {
            AnnouncementVO announcement = AnnouncementVO.builder()
                    .content(content)
                    .updatedAt(LocalDateTime.now().format(FORMATTER))
                    .build();
            redisTemplate.opsForValue().set(ANNOUNCEMENT_KEY, announcement);
            log.info("公告保存成功");
        } catch (Exception e) {
            log.error("保存公告失败", e);
            throw new RuntimeException("保存公告失败");
        }
    }
}
