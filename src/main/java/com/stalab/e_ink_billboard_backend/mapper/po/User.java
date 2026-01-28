package com.stalab.e_ink_billboard_backend.mapper.po;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 用户实体类
 * @Version: 1.0
 */
@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String wx_openid;
    private String nickname;
    private String avatar;
    private String role;
    private String loginSource;
    private String feishuOpenId;
    private String feishuUnionId;
    private String feishuEmployeeNo;
    private String feishuMobile;
    private LocalDateTime createTime;

    /**
     * 剩余免费推送次数
     * 默认为1
     */
    private Integer remainingFreePushes;
}
