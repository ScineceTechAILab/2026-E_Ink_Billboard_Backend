package com.stalab.e_ink_billboard_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import com.stalab.e_ink_billboard_backend.common.enums.DeviceStatus;
import com.stalab.e_ink_billboard_backend.mapper.ContentPushMapper;
import com.stalab.e_ink_billboard_backend.mapper.DeviceMapper;
import com.stalab.e_ink_billboard_backend.mapper.UserMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Device;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import com.stalab.e_ink_billboard_backend.model.dto.MqttCommandMessage;
import com.stalab.e_ink_billboard_backend.service.PlayQueueService.QueueItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 播放调度器
 * 定时检查播放队列，自动切换内容
 */
@Slf4j
@Component
public class PlayScheduler {

    @Autowired
    private PlayQueueService playQueueService;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private MqttService mqttService;

    @Autowired
    private MinioService minioService;

    @Autowired
    private ContentPushMapper contentPushMapper;

    @Value("${play-queue.switch-ahead-time:10}")
    private long switchAheadTime; // 提前切换时间（秒）

    /**
     * 播放轮询任务
     * 每30秒执行一次，检查所有设备的播放状态
     */
    @Scheduled(fixedDelayString = "${play-queue.poll-interval:30}000") // 转换为毫秒
    public void pollPlayQueue() {
        try {
            // 查询所有在线设备
            List<Device> onlineDevices;
            try {
                onlineDevices = deviceMapper.selectList(
                        new LambdaQueryWrapper<Device>()
                                .eq(Device::getStatus, DeviceStatus.ONLINE)
                );
            } catch (Exception e) {
                log.error("查询在线设备失败，可能是数据库连接问题", e);
                // 数据库连接失败时，不继续执行，等待下次调度
                return;
            }

            if (onlineDevices == null || onlineDevices.isEmpty()) {
                log.debug("没有在线设备");
                return;
            }

            for (Device device : onlineDevices) {
                try {
                    // 检查当前播放内容是否即将到期
                    if (playQueueService.isCurrentExpiringSoon(device.getId(), switchAheadTime)) {
                        // 从队列中取出下一个内容
                        QueueItem nextItem = playQueueService.playNext(device.getId());

                        if (nextItem != null) {
                            // 发送MQTT命令播放新内容
                            sendPlayCommand(device, nextItem);
                        } else {
                            // 队列为空，设备空闲
                            log.debug("设备播放队列为空: deviceId={}", device.getId());
                        }
                    }
                } catch (org.springframework.dao.QueryTimeoutException e) {
                    // Redis超时异常，记录警告但继续处理其他设备
                    log.warn("处理设备播放队列超时（Redis连接问题）: deviceId={}", device.getId(), e);
                } catch (org.springframework.dao.DataAccessException e) {
                    // 数据库或Redis访问异常，记录警告但继续处理其他设备
                    log.warn("处理设备播放队列失败（数据访问异常）: deviceId={}", device.getId(), e);
                } catch (Exception e) {
                    // 其他异常，记录错误但继续处理其他设备
                    log.error("处理设备播放队列失败: deviceId={}", device.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("播放轮询任务执行失败", e);
        }
    }

    /**
     * 发送播放命令
     *
     * @param device 设备
     * @param item 队列项
     */
    private void sendPlayCommand(Device device, QueueItem item) {
        try {
            // 查询推送记录获取下载URL等信息
            com.stalab.e_ink_billboard_backend.mapper.po.ContentPush push =
                    contentPushMapper.selectById(item.getPushId());
            if (push == null) {
                log.error("推送记录不存在: pushId={}", item.getPushId());
                return;
            }

            // 重新生成Presigned URL（可能已过期）
            // 注意：push.getDownloadUrl()存储的是原始URL，需要重新生成Presigned URL
            String downloadUrl;
            try {
                // 尝试从原始URL生成新的Presigned URL
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
            mqttService.publish(device.getMqttTopic(), mqttMessage);

            // 更新设备当前内容
            deviceService.updateCurrentContent(device.getId(), item.getContentId(), item.getContentType());

            // 如果是游客内容，增加播放计数
            User user = userMapper.selectById(item.getUserId());
            if (user != null && !"ADMIN".equals(user.getRole())) {
                playQueueService.incrementVisitorCount(item.getUserId(), item.getContentId());
            }

            log.info("发送播放命令: deviceId={}, contentId={}, contentType={}",
                    device.getId(), item.getContentId(), item.getContentType());
        } catch (Exception e) {
            log.error("发送播放命令失败: deviceId={}, contentId={}", device.getId(), item.getContentId(), e);
        }
    }

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private UserMapper userMapper;
}
