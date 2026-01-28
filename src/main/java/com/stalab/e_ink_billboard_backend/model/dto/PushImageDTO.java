package com.stalab.e_ink_billboard_backend.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推送图片DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushImageDTO {
    /**
     * 设备ID
     */
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    /**
     * 图片ID
     */
    @NotNull(message = "图片ID不能为空")
    private Long imageId;

    /**
     * 验证码（非管理员首次后推送需要）
     */
    private String verificationCode;
}
