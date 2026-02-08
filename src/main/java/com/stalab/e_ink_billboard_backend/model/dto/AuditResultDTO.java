package com.stalab.e_ink_billboard_backend.model.dto;

import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核结果DTO
 */
@Data
public class AuditResultDTO {

    @NotNull(message = "内容ID不能为空")
    private Long contentId;

    @NotNull(message = "内容类型不能为空")
    private ContentType contentType;

    @NotNull(message = "审核状态不能为空")
    private AuditStatus auditStatus;

    /**
     * 拒绝原因 (当状态为REJECTED时必填)
     */
    private String rejectReason;
}
