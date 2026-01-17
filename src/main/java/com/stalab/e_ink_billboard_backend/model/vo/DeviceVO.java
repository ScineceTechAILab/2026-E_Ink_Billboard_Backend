package com.stalab.e_ink_billboard_backend.model.vo;

import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import com.stalab.e_ink_billboard_backend.common.enums.DeviceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceVO {
    /**
     * 设备ID
     */
    private Long id;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * MQTT主题
     */
    private String mqttTopic;

    /**
     * 在线状态
     */
    private DeviceStatus status;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;

    /**
     * 当前显示的内容ID
     */
    private Long currentContentId;

    /**
     * 当前内容类型
     */
    private ContentType currentContentType;

    /**
     * 当前内容信息（如果正在播放内容）
     */
    private CurrentContentInfo currentContent;

    /**
     * 位置信息
     */
    private String location;

    /**
     * 设备描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 当前内容信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentContentInfo {
        /**
         * 内容ID
         */
        private Long contentId;

        /**
         * 内容类型
         */
        private ContentType contentType;

        /**
         * 文件名
         */
        private String fileName;

        /**
         * 缩略图URL（图片为原图URL，视频为第一帧或封面URL）
         */
        private String thumbnailUrl;

        /**
         * 文件大小（字节）
         */
        private Long fileSize;

        /**
         * 视频时长（秒，仅视频有）
         */
        private Integer duration;
    }
}
