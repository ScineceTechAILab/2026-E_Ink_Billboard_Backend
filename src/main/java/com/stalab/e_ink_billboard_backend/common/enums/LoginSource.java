package com.stalab.e_ink_billboard_backend.common.enums;

import lombok.Getter;

/**
 * 登录来源枚举
 */
@Getter
public enum LoginSource {
    WECHAT("WECHAT", "微信小程序"),
    FEISHU("FEISHU", "飞书应用");

    private final String code;
    private final String desc;

    LoginSource(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
