
package com.stalab.e_ink_billboard_backend.mapper.po;

import com.baomidou.mybatisplus.annotation.*;
import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审核记录表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 内容ID
     */
    private Long contentId;

    /**
     * 内容类型
     */
    private ContentType contentType;

    /**
     * 审核前状态
     */
    private AuditStatus beforeStatus;

    /**
     * 审核后状态
     */
    private AuditStatus afterStatus;

    /**
     * 审核人ID
     */
    private Long auditorId;

    /**
     * 审核人昵称
     */
    private String auditorName;

    /**
     * 审核意见/拒绝原因
     */
    private String auditReason;

    /**
     * 操作类型：AUDIT（审核）、RE_AUDIT（重新审核）
     */
    private String operationType;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
