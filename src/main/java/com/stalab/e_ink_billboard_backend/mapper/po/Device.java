package com.stalab.e_ink_billboard_backend.mapper.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import com.stalab.e_ink_billboard_backend.common.enums.DeviceStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备实体类
 * 对应数据库表：sys_device
 */
@Data
@TableName("sys_device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备名称（如：大厅屏幕1）
     */
    private String deviceName;

    /**
     * 设备编码（ESP32的MAC地址或自定义ID，唯一标识）
     */
    private String deviceCode;

    /**
     * MQTT主题（如：device/{device_code}/cmd）
     */
    private String mqttTopic;

    /**
     * 在线状态：ONLINE-在线, OFFLINE-离线
     */
    private DeviceStatus status;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;

    /**
     * 当前显示的内容ID（图片或视频）
     */
    private Long currentContentId;

    /**
     * 当前内容类型：IMAGE-图片, VIDEO-视频
     */
    private ContentType currentContentType;

    /**
     * 位置信息（可选）
     */
    private String location;

    /**
     * 设备描述（可选）
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
