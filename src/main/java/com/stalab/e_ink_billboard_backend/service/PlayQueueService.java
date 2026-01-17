package com.stalab.e_ink_billboard_backend.service;

import cn.hutool.json.JSONUtil;
import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import com.stalab.e_ink_billboard_backend.common.enums.DeviceStatus;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import com.stalab.e_ink_billboard_backend.mapper.ContentPushMapper;
import com.stalab.e_ink_billboard_backend.mapper.DeviceMapper;
import com.stalab.e_ink_billboard_backend.mapper.ImageMapper;
import com.stalab.e_ink_billboard_backend.mapper.UserMapper;
import com.stalab.e_ink_billboard_backend.mapper.VideoMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Device;
import com.stalab.e_ink_billboard_backend.mapper.po.Image;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import com.stalab.e_ink_billboard_backend.mapper.po.Video;
import com.stalab.e_ink_billboard_backend.model.dto.MqttCommandMessage;
import com.stalab.e_ink_billboard_backend.model.vo.QueueItemVO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 播放队列管理服务
 * 使用Redis实现播放队列、当前播放内容、游客限额等功能
 */
@Slf4j
@Service
public class PlayQueueService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    @Lazy
    private DeviceMapper deviceMapper;

    @Autowired
    @Lazy
    private ContentPushMapper contentPushMapper;

    @Autowired
    @Lazy
    private MqttService mqttService;

    @Autowired
    @Lazy
    private MinioService minioService;

    @Autowired
    @Lazy
    private DeviceService deviceService;

    @Autowired
    @Lazy
    private ImageMapper imageMapper;

    @Autowired
    @Lazy
    private VideoMapper videoMapper;

    @Value("${play-queue.visitor-play-duration:120}")
    private int visitorPlayDuration; // 游客内容播放时长（秒）

    @Value("${play-queue.admin-play-duration:0}")
    private int adminPlayDuration; // 管理员内容播放时长（秒，0表示不限制）

    /**
     * -- GETTER --
     *  获取游客每日限额
     */
    @Getter
    @Value("${play-queue.visitor-daily-limit:5}")
    private int visitorDailyLimit; // 游客每日最大播放数量

    // Redis Key前缀
    private static final String QUEUE_KEY_PREFIX = "device:%d:play_queue";
    private static final String CURRENT_KEY_PREFIX = "device:%d:current";
    private static final String DAILY_COUNT_KEY_PREFIX = "user:%d:daily_count:%s";
    private static final String PLAYED_SET_KEY_PREFIX = "user:%d:played_today:%s";

    // 优先级偏移量（管理员内容优先级最低）
    private static final long ADMIN_PRIORITY_OFFSET = 9999999999L;

    /**
     * 添加内容到播放队列
     *
     * @param deviceId 设备ID
     * @param contentId 内容ID
     * @param contentType 内容类型
     * @param userId 用户ID
     * @param pushId 推送记录ID
     * @param isAdmin 是否为管理员
     * @return 是否立即播放（如果队列为空则立即播放）
     */
    public boolean addToQueue(Long deviceId, Long contentId, ContentType contentType,
                             Long userId, Long pushId, boolean isAdmin) {
        String queueKey = String.format(QUEUE_KEY_PREFIX, deviceId);
        String currentKey = String.format(CURRENT_KEY_PREFIX, deviceId);

        // 构建队列成员值
        String member = String.format("content:%d:%s:%d:%d", contentId, contentType, userId, pushId);

        // 计算优先级：游客内容使用当前时间戳（越小越优先），管理员内容加上偏移量
        long score = System.currentTimeMillis();
        if (isAdmin) {
            score += ADMIN_PRIORITY_OFFSET;
        }

        // 添加到有序集合
        redisTemplate.opsForZSet().add(queueKey, member, score);

        // 检查当前是否有播放内容
        Boolean hasCurrent = redisTemplate.hasKey(currentKey);

        // 如果当前没有播放内容，立即播放
        if (!hasCurrent) {
            QueueItem item = playNext(deviceId);
            if (item != null) {
                // 立即发送MQTT消息
                sendPlayCommandImmediately(deviceId, item);
            }
            return true;
        }

        // 如果新加入的是管理员内容
        if (isAdmin) {
            QueueItem currentItem = getCurrent(deviceId);
            if (currentItem != null) {
                // 查询当前播放内容的用户角色
                User currentUser = userMapper.selectById(currentItem.getUserId());
                if (currentUser != null && "ADMIN".equals(currentUser.getRole())) {
                    // 当前播放的是管理员内容，立即打断并播放新的管理员内容
                    log.info("管理员新内容打断当前管理员内容: deviceId={}, currentContentId={}, newContentId={}",
                            deviceId, currentItem.getContentId(), contentId);

                    // 清除当前播放内容（不重新放回队列，因为管理员内容会被新内容替换）
                    redisTemplate.delete(currentKey);

                    // 移除队列中所有管理员内容（只保留游客内容，新内容已经在队列中）
                    removeAdminContentFromQueue(deviceId, pushId);

                    // 立即播放新的管理员内容
                    QueueItem item = playNext(deviceId);
                    if (item != null) {
                        // 立即发送MQTT消息
                        sendPlayCommandImmediately(deviceId, item);
                    }
                    return true;
                } else {
                    // 当前播放的是游客内容，只移除队列中的管理员内容，添加新的，但不打断当前播放
                    log.info("管理员新内容添加到队列（不打断游客内容）: deviceId={}, currentContentId={}, newContentId={}",
                            deviceId, currentItem.getContentId(), contentId);

                    // 移除队列中所有管理员内容（只保留游客内容，新内容已经在队列中）
                    removeAdminContentFromQueue(deviceId, pushId);

                    // 新内容已经在上面添加到队列了，直接返回
                    log.info("管理员内容已添加到播放队列: deviceId={}, contentId={}, contentType={}",
                            deviceId, contentId, contentType);
                    return false;
                }
            } else {
                // 当前没有播放内容，移除队列中的管理员内容后立即播放新的
                removeAdminContentFromQueue(deviceId, pushId);
                QueueItem item = playNext(deviceId);
                if (item != null) {
                    sendPlayCommandImmediately(deviceId, item);
                    return true;
                }
            }
        }

        // 如果新加入的是游客内容，检查是否需要打断当前播放的管理员内容
        if (!isAdmin) {
            QueueItem currentItem = getCurrent(deviceId);
            if (currentItem != null) {
                // 查询当前播放内容的用户角色
                User currentUser = userMapper.selectById(currentItem.getUserId());
                if (currentUser != null && "ADMIN".equals(currentUser.getRole())) {
                    // 当前播放的是管理员内容，需要打断
                    log.info("游客内容打断管理员内容: deviceId={}, currentContentId={}, newContentId={}",
                            deviceId, currentItem.getContentId(), contentId);

                    // 将当前播放的管理员内容重新放回队列（优先级最低）
                    String currentMember = (String) redisTemplate.opsForValue().get(currentKey);
                    if (currentMember != null) {
                        // 使用当前时间戳+偏移量，确保优先级最低
                        long adminScore = System.currentTimeMillis() + ADMIN_PRIORITY_OFFSET;
                        redisTemplate.opsForZSet().add(queueKey, currentMember, adminScore);
                        log.info("管理员内容已重新放回队列: deviceId={}, contentId={}",
                                deviceId, currentItem.getContentId());
                    }

                    // 清除当前播放内容
                    redisTemplate.delete(currentKey);

                    // 立即播放新的游客内容
                    QueueItem item = playNext(deviceId);
                    if (item != null) {
                        // 立即发送MQTT消息
                        sendPlayCommandImmediately(deviceId, item);
                    }
                    return true;
                }
            }
        }

        log.info("内容已添加到播放队列: deviceId={}, contentId={}, contentType={}, isAdmin={}",
                deviceId, contentId, contentType, isAdmin);
        return false;
    }

    /**
     * 播放队列中的下一个内容
     *
     * @param deviceId 设备ID
     * @return 播放的内容信息，如果队列为空返回null
     */
    public QueueItem playNext(Long deviceId) {
        String queueKey = String.format(QUEUE_KEY_PREFIX, deviceId);
        String currentKey = String.format(CURRENT_KEY_PREFIX, deviceId);

        // 从队列中取出优先级最高的内容（score最小的）
        Set<Object> members = redisTemplate.opsForZSet().range(queueKey, 0, 0);

        if (members == null || members.isEmpty()) {
            log.debug("播放队列为空: deviceId={}", deviceId);
            return null;
        }

        String member = (String) members.iterator().next();

        // 从队列中删除
        redisTemplate.opsForZSet().remove(queueKey, member);

        // 解析内容信息
        QueueItem item = parseQueueItem(member);

        // 根据用户角色设置不同的TTL
        int ttlSeconds = getPlayDuration(item.getUserId());
        if (ttlSeconds > 0) {
            // 设置当前播放内容（带TTL）
            redisTemplate.opsForValue().set(currentKey, member, ttlSeconds, TimeUnit.SECONDS);
        } else {
            // 管理员内容不限制时长（不设置TTL或设置很长的TTL）
            redisTemplate.opsForValue().set(currentKey, member);
        }

        log.info("开始播放内容: deviceId={}, contentId={}, contentType={}, userId={}, ttl={}秒",
                deviceId, item.getContentId(), item.getContentType(), item.getUserId(), ttlSeconds);

        return item;
    }

    /**
     * 获取内容的播放时长
     *
     * @param userId 用户ID
     * @return 播放时长（秒），0表示不限制
     */
    private int getPlayDuration(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user != null && "ADMIN".equals(user.getRole())) {
                return adminPlayDuration; // 管理员内容
            }
        } catch (Exception e) {
            log.warn("查询用户信息失败，默认使用游客时长: userId={}", userId, e);
        }
        return visitorPlayDuration; // 游客内容，默认2分钟
    }

    /**
     * 立即发送播放命令（用于立即播放场景）
     *
     * @param deviceId 设备ID
     * @param item 队列项
     */
    private void sendPlayCommandImmediately(Long deviceId, QueueItem item) {
        try {
            // 查询设备信息
            Device device = deviceMapper.selectById(deviceId);
            if (device == null) {
                log.error("设备不存在: deviceId={}", deviceId);
                return;
            }

            // 确保mqttTopic已设置
            if (device.getMqttTopic() == null || device.getMqttTopic().isEmpty()) {
                device.setMqttTopic("device/" + device.getDeviceCode() + "/cmd");
                log.info("自动生成MQTT主题: deviceId={}, mqttTopic={}", deviceId, device.getMqttTopic());
            }

            // 只对在线设备发送命令
            if (device.getStatus() != DeviceStatus.ONLINE) {
                log.warn("设备离线，不发送播放命令: deviceId={}, deviceCode={}, status={}, mqttTopic={}",
                        deviceId, device.getDeviceCode(), device.getStatus(), device.getMqttTopic());
                return;
            }

            log.info("准备发送播放命令: deviceId={}, deviceCode={}, mqttTopic={}, contentId={}",
                    deviceId, device.getDeviceCode(), device.getMqttTopic(), item.getContentId());

            // 查询推送记录获取下载URL等信息
            com.stalab.e_ink_billboard_backend.mapper.po.ContentPush push =
                    contentPushMapper.selectById(item.getPushId());
            if (push == null) {
                log.error("推送记录不存在: pushId={}", item.getPushId());
                return;
            }

            // 重新生成Presigned URL（可能已过期）
            String downloadUrl;
            try {
                downloadUrl = minioService.getDownloadUrl(push.getDownloadUrl());
            } catch (Exception e) {
                log.error("生成Presigned URL失败，使用原始URL: pushId={}", item.getPushId(), e);
                downloadUrl = push.getDownloadUrl();
            }

            // 构造MQTT消息
            MqttCommandMessage mqttMessage = MqttCommandMessage.builder()
                    .type(item.getContentType() == ContentType.IMAGE ? "IMAGE" : "VIDEO")
                    .contentId(item.getContentId())
                    .url(downloadUrl)
                    .size(push.getFileSize())
                    .md5(push.getMd5())
                    .timestamp(System.currentTimeMillis())
                    .messageId(push.getMqttMessageId())
                    .build();

            // 发送MQTT消息
            log.info("发送MQTT消息到主题: {}, 消息内容: {}", device.getMqttTopic(), JSONUtil.toJsonStr(mqttMessage));
            mqttService.publish(device.getMqttTopic(), mqttMessage);

            // 更新设备当前内容
            deviceService.updateCurrentContent(deviceId, item.getContentId(), item.getContentType());

            // 如果是游客内容，增加播放计数
            User user = userMapper.selectById(item.getUserId());
            if (user != null && !"ADMIN".equals(user.getRole())) {
                incrementVisitorCount(item.getUserId(), item.getContentId());
            }

            log.info("立即发送播放命令完成: deviceId={}, deviceCode={}, mqttTopic={}, contentId={}, contentType={}",
                    deviceId, device.getDeviceCode(), device.getMqttTopic(), item.getContentId(), item.getContentType());
        } catch (Exception e) {
            log.error("立即发送播放命令失败: deviceId={}, contentId={}", deviceId, item.getContentId(), e);
        }
    }

    /**
     * 检查当前播放内容是否即将到期
     *
     * @param deviceId 设备ID
     * @param aheadSeconds 提前多少秒
     * @return 如果即将到期返回true
     */
    public boolean isCurrentExpiringSoon(Long deviceId, long aheadSeconds) {
        try {
            String currentKey = String.format(CURRENT_KEY_PREFIX, deviceId);
            Long ttl = redisTemplate.getExpire(currentKey, TimeUnit.SECONDS);

            // ttl为null或-2表示键不存在，-1表示键存在但无过期时间
            if (ttl == null || ttl <= 0) {
                return true; // 键不存在或已过期
            }

            return ttl <= aheadSeconds;
        } catch (Exception e) {
            // Redis连接失败时，记录警告但不抛出异常，避免影响调度任务
            log.warn("检查播放内容到期时间失败，假设已到期: deviceId={}", deviceId, e);
            // 返回true，让调度器尝试切换内容（如果Redis恢复，会正常处理）
            return true;
        }
    }

    /**
     * 获取当前播放内容
     *
     * @param deviceId 设备ID
     * @return 当前播放内容信息，如果没有返回null
     */
    public QueueItem getCurrent(Long deviceId) {
        String currentKey = String.format(CURRENT_KEY_PREFIX, deviceId);
        Object value = redisTemplate.opsForValue().get(currentKey);

        if (value == null) {
            return null;
        }

        return parseQueueItem((String) value);
    }

    /**
     * 检查游客每日播放限额
     *
     * @param userId 用户ID
     * @return 是否可以播放（true表示可以，false表示已达上限）
     */
    public boolean checkVisitorQuota(Long userId) {
        String date = LocalDate.now().toString();
        String countKey = String.format(DAILY_COUNT_KEY_PREFIX, userId, date);

        Object countObj = redisTemplate.opsForHash().get(countKey, "count");
        int count = countObj == null ? 0 : Integer.parseInt(countObj.toString());

        if (count >= visitorDailyLimit) {
            log.warn("游客每日播放限额已达上限: userId={}, count={}, limit={}", userId, count, visitorDailyLimit);
            return false;
        }

        return true;
    }

    /**
     * 增加游客播放计数
     *
     * @param userId 用户ID
     * @param contentId 内容ID
     */
    public void incrementVisitorCount(Long userId, Long contentId) {
        String date = LocalDate.now().toString();
        String countKey = String.format(DAILY_COUNT_KEY_PREFIX, userId, date);
        String playedSetKey = String.format(PLAYED_SET_KEY_PREFIX, userId, date);

        // 检查是否已播放过此内容（防重复计数）
        Boolean isMember = redisTemplate.opsForSet().isMember(playedSetKey, contentId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            log.debug("内容已播放过，不重复计数: userId={}, contentId={}", userId, contentId);
            return;
        }

        // 增加计数
        redisTemplate.opsForHash().increment(countKey, "count", 1);
        redisTemplate.expire(countKey, Duration.ofDays(1)); // 24小时后过期

        // 添加到已播放集合
        redisTemplate.opsForSet().add(playedSetKey, contentId.toString());
        redisTemplate.expire(playedSetKey, Duration.ofDays(1)); // 24小时后过期

        log.info("增加游客播放计数: userId={}, contentId={}", userId, contentId);
    }

    /**
     * 获取游客今日播放次数
     *
     * @param userId 用户ID
     * @return 今日播放次数
     */
    public int getVisitorTodayCount(Long userId) {
        String date = LocalDate.now().toString();
        String countKey = String.format(DAILY_COUNT_KEY_PREFIX, userId, date);

        Object countObj = redisTemplate.opsForHash().get(countKey, "count");
        return countObj == null ? 0 : Integer.parseInt(countObj.toString());
    }

    /**
     * 获取队列长度
     *
     * @param deviceId 设备ID
     * @return 队列长度
     */
    public long getQueueSize(Long deviceId) {
        String queueKey = String.format(QUEUE_KEY_PREFIX, deviceId);
        Long size = redisTemplate.opsForZSet().zCard(queueKey);
        return size == null ? 0 : size;
    }

    /**
     * 移除队列中所有管理员内容（除了指定的新推送记录）
     * 当管理员上传新内容时，需要移除队列中旧的管理员内容
     *
     * @param deviceId 设备ID
     * @param newPushId 新推送记录ID（不会被移除）
     */
    private void removeAdminContentFromQueue(Long deviceId, Long newPushId) {
        String queueKey = String.format(QUEUE_KEY_PREFIX, deviceId);

        // 获取队列中所有成员（按score排序）
        Set<Object> members = redisTemplate.opsForZSet().range(queueKey, 0, -1);
        if (members == null || members.isEmpty()) {
            return;
        }

        int removedCount = 0;
        for (Object memberObj : members) {
            String member = (String) memberObj;
            try {
                // 解析队列成员，获取userId和pushId
                QueueItem item = parseQueueItem(member);
                if (item != null) {
                    // 跳过新添加的内容（通过pushId判断）
                    if (newPushId != null && newPushId.equals(item.getPushId())) {
                        continue;
                    }

                    // 查询用户角色
                    User user = userMapper.selectById(item.getUserId());
                    if (user != null && "ADMIN".equals(user.getRole())) {
                        // 从队列中移除管理员内容
                        redisTemplate.opsForZSet().remove(queueKey, member);
                        removedCount++;
                        log.debug("移除队列中的管理员内容: deviceId={}, contentId={}, contentType={}, pushId={}",
                                deviceId, item.getContentId(), item.getContentType(), item.getPushId());
                    }
                }
            } catch (Exception e) {
                log.warn("解析队列成员失败，跳过: member={}, deviceId={}", member, deviceId, e);
            }
        }

        if (removedCount > 0) {
            log.info("已移除队列中的管理员内容: deviceId={}, removedCount={}, newPushId={}", deviceId, removedCount, newPushId);
        }
    }

    /**
     * 清空设备播放队列
     *
     * @param deviceId 设备ID
     */
    public void clearQueue(Long deviceId) {
        String queueKey = String.format(QUEUE_KEY_PREFIX, deviceId);
        redisTemplate.delete(queueKey);
        log.info("清空播放队列: deviceId={}", deviceId);
    }

    /**
     * 解析队列项
     *
     * @param member 队列成员值（格式：content:{contentId}:{contentType}:{userId}:{pushId}）
     * @return 队列项信息
     */
    private QueueItem parseQueueItem(String member) {
        // 格式：content:{contentId}:{contentType}:{userId}:{pushId}
        String[] parts = member.split(":");
        if (parts.length != 5) {
            throw new BusinessException("队列项格式错误: " + member);
        }

        QueueItem item = new QueueItem();
        item.setContentId(Long.parseLong(parts[1]));
        item.setContentType(ContentType.valueOf(parts[2]));
        item.setUserId(Long.parseLong(parts[3]));
        item.setPushId(Long.parseLong(parts[4]));

        return item;
    }

    /**
     * 获取设备播放队列列表
     *
     * @param deviceId 设备ID
     * @return 队列项列表（按优先级排序）
     */
    public List<QueueItemVO> getQueueList(Long deviceId) {
        String queueKey = String.format(QUEUE_KEY_PREFIX, deviceId);

        // 获取队列中所有成员（按score排序，从小到大）
        Set<Object> members = redisTemplate.opsForZSet().range(queueKey, 0, -1);
        if (members == null || members.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        List<QueueItemVO> queueItems = new java.util.ArrayList<>();
        int position = 1;

        for (Object memberObj : members) {
            String member = (String) memberObj;
            try {
                QueueItem item = parseQueueItem(member);
                QueueItemVO vo = convertToVO(item, position++);
                if (vo != null) {
                    queueItems.add(vo);
                }
            } catch (Exception e) {
                log.warn("解析队列项失败，跳过: member={}, deviceId={}", member, deviceId, e);
            }
        }

        return queueItems;
    }

    /**
     * 删除队列中的指定项
     *
     * @param deviceId 设备ID
     * @param pushIds 要删除的推送记录ID列表
     * @return 成功删除的数量
     */
    public int deleteQueueItems(Long deviceId, List<Long> pushIds) {
        String queueKey = String.format(QUEUE_KEY_PREFIX, deviceId);

        // 获取队列中所有成员
        Set<Object> members = redisTemplate.opsForZSet().range(queueKey, 0, -1);
        if (members == null || members.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;
        for (Object memberObj : members) {
            String member = (String) memberObj;
            try {
                QueueItem item = parseQueueItem(member);
                // 如果推送记录ID在要删除的列表中，则从队列中移除
                if (pushIds.contains(item.getPushId())) {
                    redisTemplate.opsForZSet().remove(queueKey, member);
                    deletedCount++;
                    log.info("从队列中删除项: deviceId={}, pushId={}, contentId={}, contentType={}",
                            deviceId, item.getPushId(), item.getContentId(), item.getContentType());
                }
            } catch (Exception e) {
                log.warn("解析队列项失败，跳过: member={}, deviceId={}", member, deviceId, e);
            }
        }

        if (deletedCount > 0) {
            log.info("从队列中删除项完成: deviceId={}, deletedCount={}", deviceId, deletedCount);
        }

        return deletedCount;
    }

    /**
     * 将QueueItem转换为QueueItemVO
     *
     * @param item 队列项
     * @param position 队列位置
     * @return VO对象
     */
    private QueueItemVO convertToVO(QueueItem item, int position) {
        try {
            QueueItemVO.QueueItemVOBuilder builder = QueueItemVO.builder()
                    .contentId(item.getContentId())
                    .contentType(item.getContentType())
                    .pushId(item.getPushId())
                    .userId(item.getUserId())
                    .position(position);

            // 查询用户信息
            User user = userMapper.selectById(item.getUserId());
            if (user != null) {
                builder.userName(user.getNickname());
                builder.userRole(user.getRole());
            }

            // 查询内容详细信息
            if (item.getContentType() == ContentType.IMAGE) {
                Image image = imageMapper.selectById(item.getContentId());
                if (image != null) {
                    builder.fileName(image.getFileName())
                            .thumbnailUrl(image.getOriginalUrl())
                            .fileSize(image.getFileSize());
                }
            } else if (item.getContentType() == ContentType.VIDEO) {
                Video video = videoMapper.selectById(item.getContentId());
                if (video != null) {
                    builder.fileName(video.getFileName())
                            .thumbnailUrl(video.getOriginalUrl())
                            .fileSize(video.getFileSize())
                            .duration(video.getDuration());
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("转换队列项VO失败: contentId={}, contentType={}", item.getContentId(), item.getContentType(), e);
            return null;
        }
    }

    /**
     * 队列项信息
     */
    @Setter
    @Getter
    public static class QueueItem {
        private Long contentId;
        private ContentType contentType;
        private Long userId;
        private Long pushId;

    }
}
