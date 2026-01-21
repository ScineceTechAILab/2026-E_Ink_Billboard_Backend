package com.stalab.e_ink_billboard_backend.model.vo;

import lombok.Data;

/**
 * 飞书用户信息VO
 */
@Data
public class FeishuUserInfoVO {
    private String openId;
    private String unionId;
    private String name;
    private String enName;
    private String avatar;
    private String mobile;
    private String email;
    private String employeeNo;
}
