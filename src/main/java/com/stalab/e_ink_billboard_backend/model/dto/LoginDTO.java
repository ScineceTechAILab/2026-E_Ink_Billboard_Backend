package com.stalab.e_ink_billboard_backend.model.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 登录DTO
 * @Version: 1.0
 */
@Data
public class LoginDTO {
    @NotBlank(message = "Code不能为空")
    private String code;
}
