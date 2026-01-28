package com.stalab.e_ink_billboard_backend.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推送视频DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushVideoDTO {
    /**
     * 设备ID
     */
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    /**
     * 视频ID
     */
    @NotNull(message = "视频ID不能为空")
    private Long videoId;

    /**
     * 验证码（非管理员首次后推送需要）
     */
    private String verificationCode;
}
