package com.stalab.e_ink_billboard_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import com.stalab.e_ink_billboard_backend.common.enums.DeviceStatus;
import com.stalab.e_ink_billboard_backend.common.enums.UserRole;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import com.stalab.e_ink_billboard_backend.mapper.DeviceMapper;
import com.stalab.e_ink_billboard_backend.mapper.ImageMapper;
import com.stalab.e_ink_billboard_backend.mapper.VideoMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Device;
import com.stalab.e_ink_billboard_backend.mapper.po.Image;
import com.stalab.e_ink_billboard_backend.mapper.po.Video;
import com.stalab.e_ink_billboard_backend.model.vo.DeviceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备管理服务
 */
@Slf4j
@Service
public class DeviceService {

    private final DeviceMapper deviceMapper;

    @Autowired(required = false)
    private ImageMapper imageMapper;

    @Autowired(required = false)
    private VideoMapper videoMapper;

    public DeviceService(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    /**
     * 添加设备（仅管理员）
     *
     * @param device 设备信息
     * @return 保存后的设备
     */
    @Transactional(rollbackFor = Exception.class)
    public Device addDevice(Device device) {
        // 1. 检查设备编码是否已存在
        Device existDevice = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, device.getDeviceCode())
                .last("LIMIT 1"));

        if (existDevice != null) {
            throw new BusinessException("设备编码已存在: " + device.getDeviceCode());
        }

        // 2. 自动生成MQTT主题（如果未提供）
        if (device.getMqttTopic() == null || device.getMqttTopic().isEmpty()) {
            device.setMqttTopic("device/" + device.getDeviceCode() + "/cmd");
        }

        // 3. 设置默认状态
        if (device.getStatus() == null) {
            device.setStatus(DeviceStatus.OFFLINE);
        }

        // 4. 设置创建时间
        device.setCreateTime(LocalDateTime.now());
        device.setUpdateTime(LocalDateTime.now());

        // 5. 保存设备
        deviceMapper.insert(device);
        log.info("成功添加设备: deviceId={}, deviceCode={}", device.getId(), device.getDeviceCode());

        return device;
    }

    /**
     * 删除设备（仅管理员）
     * 注意：会级联删除推送记录（数据库外键约束）
     *
     * @param id 设备ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        deviceMapper.deleteById(id);
        log.info("成功删除设备: deviceId={}, deviceCode={}", id, device.getDeviceCode());
    }

    /**
     * 更新设备信息（仅管理员）
     *
     * @param device 设备信息（必须包含id）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDevice(Device device) {
        Device existDevice = deviceMapper.selectById(device.getId());
        if (existDevice == null) {
            throw new BusinessException("设备不存在");
        }

        // 如果修改了设备编码，检查新编码是否已存在
        if (device.getDeviceCode() != null && !device.getDeviceCode().equals(existDevice.getDeviceCode())) {
            Device codeExist = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                    .eq(Device::getDeviceCode, device.getDeviceCode())
                    .ne(Device::getId, device.getId())
                    .last("LIMIT 1"));

            if (codeExist != null) {
                throw new BusinessException("设备编码已存在: " + device.getDeviceCode());
            }

            // 更新MQTT主题
            device.setMqttTopic("device/" + device.getDeviceCode() + "/cmd");
        }

        // 更新更新时间
        device.setUpdateTime(LocalDateTime.now());

        deviceMapper.updateById(device);
        log.info("成功更新设备: deviceId={}", device.getId());
    }

    /**
     * 获取设备列表
     * 游客：只返回在线设备
     * 管理员：返回所有设备
     *
     * @param userId 当前用户ID
     * @param role 用户角色
     * @return 设备列表
     */
    public List<DeviceVO> getDeviceList(Long userId, String role) {
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();

        // 游客只能看到在线设备
        if (!UserRole.ADMIN.getCode().equals(role)) {
            queryWrapper.eq(Device::getStatus, DeviceStatus.ONLINE);
        }

        // 按创建时间倒序
        queryWrapper.orderByDesc(Device::getCreateTime);

        List<Device> devices = deviceMapper.selectList(queryWrapper);

        return devices.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取设备详情
     *
     * @param id 设备ID
     * @return 设备信息
     */
    public DeviceVO getDeviceById(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        return convertToVO(device);
    }

    /**
     * 更新设备状态
     *
     * @param deviceCode 设备编码
     * @param status 状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDeviceStatus(String deviceCode, DeviceStatus status) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode)
                .last("LIMIT 1"));

        if (device == null) {
            log.warn("设备不存在，无法更新状态: deviceCode={}", deviceCode);
            return;
        }

        device.setStatus(status);
        device.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(device);

        log.info("更新设备状态: deviceCode={}, status={}", deviceCode, status);
    }

    /**
     * 更新心跳时间
     *
     * @param deviceCode 设备编码
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateHeartbeat(String deviceCode) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode)
                .last("LIMIT 1"));

        if (device == null) {
            log.warn("设备不存在，无法更新心跳: deviceCode={}", deviceCode);
            return;
        }

        device.setLastHeartbeat(LocalDateTime.now());
        device.setStatus(DeviceStatus.ONLINE); // 收到心跳说明设备在线
        device.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(device);

        log.debug("更新设备心跳: deviceCode={}", deviceCode);
    }

    /**
     * 更新当前显示内容
     *
     * @param deviceId 设备ID
     * @param contentId 内容ID
     * @param contentType 内容类型
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentContent(Long deviceId, Long contentId, ContentType contentType) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            log.warn("设备不存在，无法更新当前内容: deviceId={}", deviceId);
            return;
        }

        device.setCurrentContentId(contentId);
        device.setCurrentContentType(contentType);
        device.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(device);

        log.info("更新设备当前内容: deviceId={}, contentId={}, contentType={}", deviceId, contentId, contentType);
    }

    /**
     * 将Device实体转换为DeviceVO
     */
    private DeviceVO convertToVO(Device device) {
        DeviceVO.DeviceVOBuilder builder = DeviceVO.builder()
                .id(device.getId())
                .deviceName(device.getDeviceName())
                .deviceCode(device.getDeviceCode())
                .mqttTopic(device.getMqttTopic())
                .status(device.getStatus())
                .lastHeartbeat(device.getLastHeartbeat())
                .currentContentId(device.getCurrentContentId())
                .currentContentType(device.getCurrentContentType())
                .location(device.getLocation())
                .description(device.getDescription())
                .createTime(device.getCreateTime());

        // 如果当前有播放内容，查询内容详细信息
        if (device.getCurrentContentId() != null && device.getCurrentContentType() != null) {
            DeviceVO.CurrentContentInfo contentInfo = getCurrentContentInfo(
                    device.getCurrentContentId(),
                    device.getCurrentContentType()
            );
            builder.currentContent(contentInfo);
        }

        return builder.build();
    }

    /**
     * 获取当前内容详细信息
     *
     * @param contentId 内容ID
     * @param contentType 内容类型
     * @return 内容信息，如果不存在返回null
     */
    private DeviceVO.CurrentContentInfo getCurrentContentInfo(Long contentId, ContentType contentType) {
        try {
            if (contentType == ContentType.IMAGE) {
                if (imageMapper == null) {
                    log.warn("ImageMapper未注入，无法查询图片信息");
                    return null;
                }
                Image image = imageMapper.selectById(contentId);
                if (image == null) {
                    log.warn("图片不存在: contentId={}", contentId);
                    return null;
                }
                return DeviceVO.CurrentContentInfo.builder()
                        .contentId(image.getId())
                        .contentType(ContentType.IMAGE)
                        .fileName(image.getFileName())
                        .thumbnailUrl(image.getOriginalUrl()) // 使用原图作为缩略图
                        .fileSize(image.getFileSize())
                        .build();
            } else if (contentType == ContentType.VIDEO) {
                if (videoMapper == null) {
                    log.warn("VideoMapper未注入，无法查询视频信息");
                    return null;
                }
                Video video = videoMapper.selectById(contentId);
                if (video == null) {
                    log.warn("视频不存在: contentId={}", contentId);
                    return null;
                }
                return DeviceVO.CurrentContentInfo.builder()
                        .contentId(video.getId())
                        .contentType(ContentType.VIDEO)
                        .fileName(video.getFileName())
                        .thumbnailUrl(video.getOriginalUrl()) // 视频暂时使用原视频URL，后续可以添加封面图
                        .fileSize(video.getFileSize())
                        .duration(video.getDuration())
                        .build();
            }
        } catch (Exception e) {
            log.error("查询当前内容信息失败: contentId={}, contentType={}", contentId, contentType, e);
        }
        return null;
    }
}
