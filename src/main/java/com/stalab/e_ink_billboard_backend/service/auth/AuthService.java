package com.stalab.e_ink_billboard_backend.service.auth;

import com.stalab.e_ink_billboard_backend.model.vo.LoginVO;

/**
 * 登录服务抽象接口
 */
public interface AuthService {
    /**
     * 使用授权码登录
     * @param code 第三方登录授权码
     * @return 登录结果
     */
    LoginVO login(String code);
}
