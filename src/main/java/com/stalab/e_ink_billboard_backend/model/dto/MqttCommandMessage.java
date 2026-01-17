package com.stalab.e_ink_billboard_backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT命令消息（后端 → ESP32）
 * 用于推送图片或视频到设备
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttCommandMessage {

    /**
     * 内容类型：IMAGE 或 VIDEO
     */
    private String type;

    /**
     * 内容ID（图片ID或视频ID）
     */
    private Long contentId;

    /**
     * 下载URL（Presigned URL）
     */
    private String url;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 文件MD5校验码
     */
    private String md5;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 消息ID（用于确认）
     */
    private String messageId;
}
