package com.stalab.e_ink_billboard_backend.model.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 登录VO
 * @Version: 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginVO {
    private String nickname;
    private String role;
    private String token;
}
