package com.stalab.e_ink_billboard_backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 图片上传响应VO
 * @Version: 1.0
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
     * 原图URL
     */
    private String url;
}
