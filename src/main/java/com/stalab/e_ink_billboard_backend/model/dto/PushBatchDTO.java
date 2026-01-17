package com.stalab.e_ink_billboard_backend.model.dto;

import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量推送DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushBatchDTO {
    /**
     * 设备ID列表
     */
    @NotEmpty(message = "设备ID列表不能为空")
    private List<Long> deviceIds;

    /**
     * 内容ID（图片或视频）
     */
    @NotNull(message = "内容ID不能为空")
    private Long contentId;

    /**
     * 内容类型：IMAGE 或 VIDEO
     */
    @NotNull(message = "内容类型不能为空")
    private ContentType contentType;
}
