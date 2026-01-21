package com.stalab.e_ink_billboard_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 飞书应用配置
 */
@Configuration
@ConfigurationProperties(prefix = "feishu")
@Data
public class FeishuConfig {
    /**
     * 飞书应用App ID
     */
    private String appId;

    /**
     * 飞书应用App Secret
     */
    private String appSecret;

    /**
     * OAuth授权地址
     */
    private String oauthAuthorizeUrl;

    /**
     * 获取access_token地址
     */
    private String oauthTokenUrl;

    /**
     * 获取用户信息地址
     */
    private String userInfoUrl;

    /**
     * OAuth 重定向地址（与飞书应用后台配置一致）
     */
    private String redirectUri;
}
