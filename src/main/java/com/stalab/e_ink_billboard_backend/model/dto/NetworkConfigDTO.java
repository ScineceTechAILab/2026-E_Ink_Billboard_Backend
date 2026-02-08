package com.stalab.e_ink_billboard_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 配网配置DTO
 */
@Data
public class NetworkConfigDTO {

    @NotBlank(message = "WiFi名称不能为空")
    private String ssid;

    @NotBlank(message = "WiFi密码不能为空")
    private String password;
}
