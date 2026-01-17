package com.stalab.e_ink_billboard_backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT心跳消息（ESP32 → 后端）
 * 用于上报设备在线状态和当前显示内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttHeartbeatMessage {

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * 设备状态：ONLINE
     */
    private String status;

    /**
     * 当前显示的内容ID
     */
    private Long currentContentId;

    /**
     * 当前内容类型：IMAGE 或 VIDEO
     */
    private String currentContentType;

    /**
     * 电池电量（可选，0-100）
     */
    private Integer battery;

    /**
     * WiFi信号强度（可选，dBm，如-65）
     */
    private Integer signal;

    /**
     * 时间戳
     */
    private Long timestamp;
}
