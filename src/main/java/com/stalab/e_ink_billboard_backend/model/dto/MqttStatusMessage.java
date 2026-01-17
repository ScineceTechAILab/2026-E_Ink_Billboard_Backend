package com.stalab.e_ink_billboard_backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT状态上报消息（ESP32 → 后端）
 * 用于上报推送命令的执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttStatusMessage {

    /**
     * 状态：SUCCESS（成功）、FAILED（失败）、DOWNLOADING（下载中）、DISPLAYING（显示中）
     */
    private String status;

    /**
     * 对应的命令消息ID
     */
    private String messageId;

    /**
     * 错误信息（失败时才有）
     */
    private String error;

    /**
     * 时间戳
     */
    private Long timestamp;
}
