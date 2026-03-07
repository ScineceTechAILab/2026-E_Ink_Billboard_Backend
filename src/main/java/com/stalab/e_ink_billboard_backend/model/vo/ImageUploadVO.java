package com.stalab.e_ink_billboard_backend.model.vo;

import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片上传响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadVO {
    /**
     * 图片ID
     */
    private Long id;

    /**
     * 处理后图片URL
     */
    private String url;

    /**
     * 审核状态
     */
    private AuditStatus auditStatus;

    /**
     * 审核信息提示
     */
    private String auditMessage;
}
