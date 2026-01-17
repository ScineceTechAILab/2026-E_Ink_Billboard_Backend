package com.stalab.e_ink_billboard_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 对所有路径生效
                .allowedOrigins("*") // 允许所有源地址（生产环境建议指定具体域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的请求方式
                .allowedHeaders("*") // 允许的请求头
                .allowCredentials(false); // 是否允许携带 Cookie (如果设为true, allowedOrigins不能为*)
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 允许URL末尾有斜杠，自动匹配
        configurer.setUseTrailingSlashMatch(true);
    }
}
