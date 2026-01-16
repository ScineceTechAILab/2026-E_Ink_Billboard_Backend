package com.stalab.e_ink_billboard_backend.model.vo;

import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 图片列表VO
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageVO {
    /**
     * 图片ID
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
     * 原图URL
     */
    private String originalUrl;

    /**
     * 处理后的图片URL
     */
    private String processedUrl;

    /**
     * 审核状态
     */
    private AuditStatus auditStatus;

    /**
     * 审核原因
     */
    private String auditReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
