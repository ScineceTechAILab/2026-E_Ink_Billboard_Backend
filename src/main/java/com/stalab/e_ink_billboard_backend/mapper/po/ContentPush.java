package com.stalab.e_ink_billboard_backend.mapper.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import com.stalab.e_ink_billboard_backend.common.enums.PushStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容推送记录实体类
 * 对应数据库表：sys_content_push
 */
@Data
@TableName("sys_content_push")
public class ContentPush {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 内容ID（图片或视频）
     */
    private Long contentId;

    /**
     * 内容类型：IMAGE-图片, VIDEO-视频
     */
    private ContentType contentType;

    /**
     * 推送状态：PENDING-待发送, SENT-已发送, SUCCESS-成功, FAILED-失败
     */
    private PushStatus pushStatus;

    /**
     * MQTT消息ID（用于确认）
     */
    private String mqttMessageId;

    /**
     * 推送时间
     */
    private LocalDateTime pushTime;

    /**
     * 推送用户ID
     */
    private Long userId;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 下载URL（Presigned URL，用于记录）
     */
    private String downloadUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件MD5校验码
     */
    private String md5;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
