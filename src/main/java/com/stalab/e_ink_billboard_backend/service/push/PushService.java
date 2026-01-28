package com.stalab.e_ink_billboard_backend.service.push;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import com.stalab.e_ink_billboard_backend.common.enums.DeviceStatus;
import com.stalab.e_ink_billboard_backend.common.enums.PushStatus;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import com.stalab.e_ink_billboard_backend.mapper.*;
import com.stalab.e_ink_billboard_backend.mapper.po.ContentPush;
import com.stalab.e_ink_billboard_backend.mapper.po.Device;
import com.stalab.e_ink_billboard_backend.mapper.po.Image;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import com.stalab.e_ink_billboard_backend.mapper.po.Video;
import com.stalab.e_ink_billboard_backend.model.vo.ContentPushVO;
import com.stalab.e_ink_billboard_backend.model.vo.PageResult;
import com.stalab.e_ink_billboard_backend.service.DeviceService;
import com.stalab.e_ink_billboard_backend.service.mqtt.MqttService;
import com.stalab.e_ink_billboard_backend.service.storage.MinioService;
import com.stalab.e_ink_billboard_backend.service.wx.VerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 内容推送服务
 */
@Slf4j
@Service
public class PushService {

    private final DeviceMapper deviceMapper;
    private final ImageMapper imageMapper;
    private final VideoMapper videoMapper;
    private final ContentPushMapper contentPushMapper;
    private final UserMapper userMapper;
    private final MqttService mqttService;
    private final MinioService minioService;
    private final PlayQueueService playQueueService;
    private final DeviceService deviceService;
    private final VerificationService verificationService;

    public PushService(DeviceMapper deviceMapper, ImageMapper imageMapper, VideoMapper videoMapper,
                      ContentPushMapper contentPushMapper, UserMapper userMapper,
                      MqttService mqttService, MinioService minioService, PlayQueueService playQueueService,
                      DeviceService deviceService, VerificationService verificationService) {
        this.deviceMapper = deviceMapper;
        this.imageMapper = imageMapper;
        this.videoMapper = videoMapper;
        this.contentPushMapper = contentPushMapper;
        this.userMapper = userMapper;
        this.mqttService = mqttService;
        this.minioService = minioService;
        this.playQueueService = playQueueService;
        this.deviceService = deviceService;
        this.verificationService = verificationService;
    }

    /**
     * 推送图片到设备
     *
     * @param deviceId 设备ID
     * @param imageId 图片ID
     * @param userId 用户ID
     * @param userRole 用户角色
     * @param verificationCode 验证码（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void pushImage(Long deviceId, Long imageId, Long userId, String userRole, String verificationCode) {
        // 1. 验证设备存在
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        // 2. 游客只能推送到在线设备
        if (!"ADMIN".equals(userRole) && device.getStatus() != DeviceStatus.ONLINE) {
            throw new BusinessException("设备离线，无法推送");
        }

        // 3. 验证图片存在且已审核通过
        Image image = imageMapper.selectById(imageId);
        if (image == null) {
            throw new BusinessException("图片不存在");
        }

        // 4. 权限检查
        boolean isAdmin = "ADMIN".equals(userRole);
        if (!isAdmin) {
            if (!image.getUserId().equals(userId)) {
                throw new BusinessException("无权推送此图片");
            }
            if (image.getAuditStatus() != AuditStatus.APPROVED) {
                throw new BusinessException("图片未审核通过，无法推送");
            }

            // 5. 检查游客每日播放限额
            if (!playQueueService.checkVisitorQuota(userId)) {
                int todayCount = playQueueService.getVisitorTodayCount(userId);
                throw new BusinessException(String.format("今日播放次数已达上限（%d/%d），请明天再试",
                        todayCount, playQueueService.getVisitorDailyLimit()));
            }

            // ★★★ 6. 检查免费配额或验证码 ★★★
            checkQuotaOrVerification(userId, verificationCode);

        } else {
            // 管理员只能推送已审核通过的内容
            if (image.getAuditStatus() != AuditStatus.APPROVED) {
                throw new BusinessException("图片未审核通过，无法推送");
            }
        }

        // 7. 保存推送记录（存储永久URL，发送时再生成Presigned URL）
        String messageId = UUID.fastUUID().toString(true);
        ContentPush pushRecord = new ContentPush();
        pushRecord.setDeviceId(deviceId);
        pushRecord.setContentId(imageId);
        pushRecord.setContentType(ContentType.IMAGE);
        pushRecord.setPushStatus(PushStatus.PENDING); // 改为PENDING，等待队列播放
        pushRecord.setMqttMessageId(messageId);
        pushRecord.setPushTime(LocalDateTime.now());
        pushRecord.setUserId(userId);
        // 存储永久URL，而不是Presigned URL（Presigned URL会在发送时动态生成）
        pushRecord.setDownloadUrl(image.getProcessedUrl());
        pushRecord.setFileSize(image.getFileSize());
        pushRecord.setMd5(image.getMd5());
        pushRecord.setCreateTime(LocalDateTime.now());
        pushRecord.setUpdateTime(LocalDateTime.now());
        contentPushMapper.insert(pushRecord);

        // 8. 添加到播放队列
        boolean immediatePlay = playQueueService.addToQueue(deviceId, imageId, ContentType.IMAGE, userId, pushRecord.getId(), isAdmin);

        if (immediatePlay) {
            // 如果立即播放，更新状态为SENT
            pushRecord.setPushStatus(PushStatus.SENT);
            contentPushMapper.updateById(pushRecord);
        }

        log.info("图片已添加到播放队列: deviceId={}, imageId={}, userId={}, pushId={}, isAdmin={}, immediatePlay={}",
                deviceId, imageId, userId, pushRecord.getId(), isAdmin, immediatePlay);
    }

    /**
     * 推送视频到设备
     *
     * @param deviceId 设备ID
     * @param videoId 视频ID
     * @param userId 用户ID
     * @param userRole 用户角色
     * @param verificationCode 验证码（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void pushVideo(Long deviceId, Long videoId, Long userId, String userRole, String verificationCode) {
        // 1. 验证设备存在
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        // 2. 游客只能推送到在线设备
        if (!"ADMIN".equals(userRole) && device.getStatus() != DeviceStatus.ONLINE) {
            throw new BusinessException("设备离线，无法推送");
        }

        // 3. 验证视频存在且已处理成功
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }

        // 4. 检查视频处理状态
        if (!"SUCCESS".equals(video.getProcessingStatus())) {
            throw new BusinessException("视频处理未完成，无法推送");
        }

        // 5. 权限检查
        boolean isAdmin = "ADMIN".equals(userRole);
        if (!isAdmin) {
            if (!video.getUserId().equals(userId)) {
                throw new BusinessException("无权推送此视频");
            }
            if (!"APPROVED".equals(video.getAuditStatus())) {
                throw new BusinessException("视频未审核通过，无法推送");
            }

            // 6. 检查游客每日播放限额
            if (!playQueueService.checkVisitorQuota(userId)) {
                int todayCount = playQueueService.getVisitorTodayCount(userId);
                throw new BusinessException(String.format("今日播放次数已达上限（%d/%d），请明天再试",
                        todayCount, playQueueService.getVisitorDailyLimit()));
            }

            // ★★★ 7. 检查免费配额或验证码 ★★★
            checkQuotaOrVerification(userId, verificationCode);

        } else {
            // 管理员只能推送已审核通过的内容
            if (!"APPROVED".equals(video.getAuditStatus())) {
                throw new BusinessException("视频未审核通过，无法推送");
            }
        }

        // 8. 保存推送记录（存储永久URL，发送时再生成Presigned URL）
        String messageId = UUID.fastUUID().toString(true);
        ContentPush pushRecord = new ContentPush();
        pushRecord.setDeviceId(deviceId);
        pushRecord.setContentId(videoId);
        pushRecord.setContentType(ContentType.VIDEO);
        pushRecord.setPushStatus(PushStatus.PENDING); // 改为PENDING，等待队列播放
        pushRecord.setMqttMessageId(messageId);
        pushRecord.setPushTime(LocalDateTime.now());
        pushRecord.setUserId(userId);
        // 存储永久URL，而不是Presigned URL（Presigned URL会在发送时动态生成）
        pushRecord.setDownloadUrl(video.getProcessedUrl());
        pushRecord.setFileSize(video.getFileSize());
        pushRecord.setMd5(null);
        pushRecord.setCreateTime(LocalDateTime.now());
        pushRecord.setUpdateTime(LocalDateTime.now());
        contentPushMapper.insert(pushRecord);

        // 9. 添加到播放队列
        boolean immediatePlay = playQueueService.addToQueue(deviceId, videoId, ContentType.VIDEO, userId, pushRecord.getId(), isAdmin);

        if (immediatePlay) {
            // 如果立即播放，更新状态为SENT
            pushRecord.setPushStatus(PushStatus.SENT);
            contentPushMapper.updateById(pushRecord);
        }

        log.info("视频已添加到播放队列: deviceId={}, videoId={}, userId={}, pushId={}, isAdmin={}, immediatePlay={}",
                deviceId, videoId, userId, pushRecord.getId(), isAdmin, immediatePlay);
    }

    /**
     * 检查用户配额或验证码
     * 逻辑：
     * 1. 检查是否还有免费次数
     * 2. 如果有，扣减次数并放行
     * 3. 如果没有，检查验证码
     */
    private void checkQuotaOrVerification(Long userId, String verificationCode) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        // 默认如果没有字段值，视为有1次机会（兼容旧数据）
        int remaining = user.getRemainingFreePushes() == null ? 1 : user.getRemainingFreePushes();

        if (remaining > 0) {
            // 消耗一次免费机会
            user.setRemainingFreePushes(remaining - 1);
            userMapper.updateById(user);
            log.info("用户消耗一次免费推送机会: userId={}", userId);
        } else {
            // 免费机会已用完，需要验证码
            if (verificationCode == null || verificationCode.isEmpty()) {
                throw new BusinessException("免费推送次数已用完，请关注微信公众号发送“验证码”获取权限");
            }

            // 验证验证码
            boolean isValid = verificationService.validateAndConsumeCode(verificationCode);
            if (!isValid) {
                throw new BusinessException("验证码无效或已过期，请重新获取");
            }
            log.info("用户使用验证码推送成功: userId={}, code={}", userId, verificationCode);
        }
    }

    /**
     * 批量推送到多个设备
     *
     * @param deviceIds 设备ID列表
     * @param contentId 内容ID
     * @param contentType 内容类型
     * @param userId 用户ID
     * @param userRole 用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void pushBatch(List<Long> deviceIds, Long contentId, ContentType contentType, Long userId, String userRole) {
        // 批量推送目前简化处理，不进行验证码校验（假设是管理员功能）
        for (Long deviceId : deviceIds) {
            try {
                if (contentType == ContentType.IMAGE) {
                    // 内部调用，为了避免重复校验验证码（而且这里没传验证码），如果是游客批量推送，可能需要额外逻辑
                    // 这里直接调用pushImage，但传入null验证码。如果游客没次数会失败。
                    // 建议：批量推送应仅限管理员
                    if (!"ADMIN".equals(userRole)) {
                        throw new BusinessException("只有管理员可以使用批量推送");
                    }
                    pushImage(deviceId, contentId, userId, userRole, null);
                } else if (contentType == ContentType.VIDEO) {
                    if (!"ADMIN".equals(userRole)) {
                        throw new BusinessException("只有管理员可以使用批量推送");
                    }
                    pushVideo(deviceId, contentId, userId, userRole, null);
                } else {
                    throw new BusinessException("不支持的内容类型: " + contentType);
                }
            } catch (Exception e) {
                log.error("批量推送失败: deviceId={}, contentId={}, contentType={}", deviceId, contentId, contentType, e);
                // 继续推送其他设备，不中断
            }
        }
    }

    /**
     * 查询推送历史（分页）
     *
     * @param current 当前页码
     * @param size 每页大小
     * @param deviceId 设备ID（可选）
     * @param userId 用户ID（可选，管理员可用）
     * @param currentUserId 当前用户ID
     * @param userRole 当前用户角色
     * @return 分页结果
     */
    public PageResult<ContentPushVO> getPushHistory(Long current, Long size, Long deviceId, Long userId,
                                                     Long currentUserId, String userRole) {
        // 默认值
        if (current == null || current < 1) {
            current = 1L;
        }
        if (size == null || size < 1) {
            size = 10L;
        }
        if (size > 100) {
            size = 100L;
        }

        // 构建查询条件
        LambdaQueryWrapper<ContentPush> queryWrapper = new LambdaQueryWrapper<>();
        if (deviceId != null) {
            queryWrapper.eq(ContentPush::getDeviceId, deviceId);
        }

        // 权限控制：游客只能查看自己的推送记录
        if (!"ADMIN".equals(userRole)) {
            queryWrapper.eq(ContentPush::getUserId, currentUserId);
        } else if (userId != null) {
            // 管理员可以查看指定用户的推送记录
            queryWrapper.eq(ContentPush::getUserId, userId);
        }

        // 按推送时间倒序
        queryWrapper.orderByDesc(ContentPush::getPushTime);

        // 分页查询
        Page<ContentPush> page = new Page<>(current, size);
        IPage<ContentPush> pageResult = contentPushMapper.selectPage(page, queryWrapper);

        // 转换为VO
        List<ContentPushVO> voList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 构建分页结果
        return PageResult.<ContentPushVO>builder()
                .records(voList)
                .total(pageResult.getTotal())
                .current(pageResult.getCurrent())
                .size(pageResult.getSize())
                .pages(pageResult.getPages())
                .build();
    }

    /**
     * 处理ESP32状态上报消息
     *
     * @param deviceCode 设备编码（从topic中提取）
     * @param statusMessage 状态消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleStatusMessage(String deviceCode, com.stalab.e_ink_billboard_backend.model.dto.MqttStatusMessage statusMessage) {
        try {
            log.info("收到设备状态上报: deviceCode={}, status={}, messageId={}",
                    deviceCode, statusMessage.getStatus(), statusMessage.getMessageId());

            // 1. 根据messageId查找推送记录
            if (statusMessage.getMessageId() == null || statusMessage.getMessageId().isEmpty()) {
                log.warn("状态消息缺少messageId，无法更新推送记录: deviceCode={}", deviceCode);
                return;
            }

            ContentPush pushRecord = contentPushMapper.selectOne(
                    new LambdaQueryWrapper<ContentPush>()
                            .eq(ContentPush::getMqttMessageId, statusMessage.getMessageId())
                            .last("LIMIT 1")
            );

            if (pushRecord == null) {
                log.warn("未找到对应的推送记录: messageId={}", statusMessage.getMessageId());
                return;
            }

            // 2. 更新推送记录状态
            PushStatus newStatus;
            if ("SUCCESS".equals(statusMessage.getStatus())) {
                newStatus = PushStatus.SUCCESS;
            } else if ("FAILED".equals(statusMessage.getStatus())) {
                newStatus = PushStatus.FAILED;
            } else {
                // DOWNLOADING, DISPLAYING 等中间状态，保持SENT状态
                log.debug("收到中间状态，不更新推送记录: status={}, messageId={}",
                        statusMessage.getStatus(), statusMessage.getMessageId());
                return;
            }

            pushRecord.setPushStatus(newStatus);
            if (statusMessage.getError() != null && !statusMessage.getError().isEmpty()) {
                pushRecord.setErrorMessage(statusMessage.getError());
            }
            pushRecord.setUpdateTime(LocalDateTime.now());
            contentPushMapper.updateById(pushRecord);

            // 3. 如果成功，更新设备的当前显示内容
            if (newStatus == PushStatus.SUCCESS) {
                Device device = deviceMapper.selectOne(
                        new LambdaQueryWrapper<Device>()
                                .eq(Device::getDeviceCode, deviceCode)
                                .last("LIMIT 1")
                );

                if (device != null) {
                    deviceService.updateCurrentContent(
                            device.getId(),
                            pushRecord.getContentId(),
                            pushRecord.getContentType()
                    );
                    log.info("更新设备当前内容: deviceId={}, contentId={}, contentType={}",
                            device.getId(), pushRecord.getContentId(), pushRecord.getContentType());
                }
            }

            log.info("处理状态上报完成: deviceCode={}, messageId={}, status={}",
                    deviceCode, statusMessage.getMessageId(), newStatus);
        } catch (Exception e) {
            log.error("处理状态上报失败: deviceCode={}, messageId={}",
                    deviceCode, statusMessage.getMessageId(), e);
        }
    }

    /**
     * 处理ESP32心跳消息
     *
     * @param deviceCode 设备编码（从topic中提取）
     * @param heartbeatMessage 心跳消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleHeartbeat(String deviceCode, com.stalab.e_ink_billboard_backend.model.dto.MqttHeartbeatMessage heartbeatMessage) {
        try {
            log.info("收到设备心跳: deviceCode={}, currentContentId={}",
                    deviceCode, heartbeatMessage.getCurrentContentId());

            // 1. 更新设备心跳时间和状态
            deviceService.updateHeartbeat(deviceCode);
            log.info("设备心跳已更新: deviceCode={}", deviceCode);

            // 2. 如果ESP32上报了当前显示内容，更新数据库
            if (heartbeatMessage.getCurrentContentId() != null && heartbeatMessage.getCurrentContentType() != null) {
                Device device = deviceMapper.selectOne(
                        new LambdaQueryWrapper<Device>()
                                .eq(Device::getDeviceCode, deviceCode)
                                .last("LIMIT 1")
                );

                if (device != null) {
                    try {
                        ContentType contentType = ContentType.valueOf(heartbeatMessage.getCurrentContentType());
                        deviceService.updateCurrentContent(
                                device.getId(),
                                heartbeatMessage.getCurrentContentId(),
                                contentType
                        );
                        log.debug("根据心跳更新设备当前内容: deviceId={}, contentId={}, contentType={}",
                                device.getId(), heartbeatMessage.getCurrentContentId(), contentType);
                    } catch (IllegalArgumentException e) {
                        log.warn("心跳消息中的内容类型无效: contentType={}, deviceCode={}",
                                heartbeatMessage.getCurrentContentType(), deviceCode);
                    }
                }
            }

            // 3. 可以在这里记录电池电量和信号强度等信息（如果数据库有字段）
            // 目前Device实体没有这些字段，暂时不处理

        } catch (Exception e) {
            log.error("处理心跳消息失败: deviceCode={}", deviceCode, e);
        }
    }

    /**
     * 将ContentPush实体转换为ContentPushVO
     */
    private ContentPushVO convertToVO(ContentPush push) {
        // 查询设备名称
        String deviceName = null;
        if (push.getDeviceId() != null) {
            Device device = deviceMapper.selectById(push.getDeviceId());
            if (device != null) {
                deviceName = device.getDeviceName();
            }
        }

        // 查询用户昵称
        String userName = null;
        if (push.getUserId() != null) {
            User user = userMapper.selectById(push.getUserId());
            if (user != null) {
                userName = user.getNickname();
            }
        }

        return ContentPushVO.builder()
                .id(push.getId())
                .deviceId(push.getDeviceId())
                .deviceName(deviceName)
                .contentId(push.getContentId())
                .contentType(push.getContentType())
                .pushStatus(push.getPushStatus())
                .pushTime(push.getPushTime())
                .userId(push.getUserId())
                .userName(userName)
                .errorMessage(push.getErrorMessage())
                .build();
    }
}
