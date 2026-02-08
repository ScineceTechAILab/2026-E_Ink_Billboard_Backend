package com.stalab.e_ink_billboard_backend.model.vo;

import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审核项VO
 * 统一展示图片和视频的待审核信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditItemVO {

    /**
     * 内容ID
     */
    private Long id;

    /**
     * 内容类型 (IMAGE/VIDEO)
     */
    private ContentType contentType;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 原始文件URL (用于预览)
     */
    private String originalUrl;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 审核状态 (应该是PENDING)
     */
    private AuditStatus auditStatus;

    /**
     * 审核原因 (例如: WeChat Check Failed)
     */
    private String auditReason;

    /**
     * 提交时间
     */
    private LocalDateTime createTime;
}
