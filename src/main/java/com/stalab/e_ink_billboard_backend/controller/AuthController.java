package com.stalab.e_ink_billboard_backend.controller;


import com.stalab.e_ink_billboard_backend.common.Response;
import com.stalab.e_ink_billboard_backend.model.dto.FeishuLoginDTO;
import com.stalab.e_ink_billboard_backend.model.dto.LoginDTO;
import com.stalab.e_ink_billboard_backend.model.vo.LoginVO;
import com.stalab.e_ink_billboard_backend.service.auth.AuthService;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 登录接口
 * @Version: 1.0
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService wechatAuthService;
    private final AuthService feishuAuthService;

    public AuthController(@Qualifier("wechatAuthService") AuthService wechatAuthService,
                          @Qualifier("feishuAuthService") AuthService feishuAuthService) {
        this.wechatAuthService = wechatAuthService;
        this.feishuAuthService = feishuAuthService;
    }

    @PostMapping("/login")
    public Response<LoginVO> login(@RequestBody LoginDTO loginDto) {
        // loginDto 里面只有一个 String code
        LoginVO vo = wechatAuthService.login(loginDto.getCode());
        return Response.<LoginVO>builder()
                .code(200)
                .info("登录成功")
                .data(vo)
                .build();
    }

    /**
     * 飞书登录接口（管理员专用）
     */
    @PostMapping("/feishu/login")
    public Response<LoginVO> feishuLogin(@RequestBody FeishuLoginDTO loginDto) {
        LoginVO vo = feishuAuthService.login(loginDto.getCode());
        return Response.<LoginVO>builder()
                .code(200)
                .info("飞书登录成功")
                .data(vo)
                .build();
    }
}
