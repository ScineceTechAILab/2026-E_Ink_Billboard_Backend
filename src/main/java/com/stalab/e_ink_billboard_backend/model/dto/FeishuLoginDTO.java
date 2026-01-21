package com.stalab.e_ink_billboard_backend.model.dto;

import lombok.Data;

/**
 * 飞书登录请求DTO
 */
@Data
public class FeishuLoginDTO {
    /**
     * 飞书授权码
     */
    private String code;
}
