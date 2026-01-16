package com.stalab.e_ink_billboard_backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 视频列表VO
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoVO {
    /**
     * 视频ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 原视频URL
     */
    private String originalUrl;

    /**
     * 处理后的视频URL
     */
    private String processedUrl;

    /**
     * 时长（秒）
     */
    private Integer duration;

    /**
     * 总帧数
     */
    private Integer frameCount;

    /**
     * 审核状态
     */
    private String auditStatus;

    /**
     * 处理状态
     */
    private String processingStatus;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
