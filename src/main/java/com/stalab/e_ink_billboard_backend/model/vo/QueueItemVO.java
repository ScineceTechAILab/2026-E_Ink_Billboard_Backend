package com.stalab.e_ink_billboard_backend.model.vo;

import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 播放队列项VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueItemVO {
    /**
     * 内容ID
     */
    private Long contentId;

    /**
     * 内容类型
     */
    private ContentType contentType;

    /**
     * 推送记录ID
     */
    private Long pushId;

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

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户角色（ADMIN/VISITOR）
     */
    private String userRole;

    /**
     * 队列中的位置（从1开始，1表示下一个播放）
     */
    private Integer position;
}
