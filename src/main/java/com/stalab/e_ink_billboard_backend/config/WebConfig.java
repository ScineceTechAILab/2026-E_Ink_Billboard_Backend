package com.stalab.e_ink_billboard_backend.config;

import com.stalab.e_ink_billboard_backend.interceptor.UserActivityInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UserActivityInterceptor userActivityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册用户活跃度拦截器，拦截所有 API 请求
        registry.addInterceptor(userActivityInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**"); // 排除登录接口，因为登录接口本身会更新时间
    }

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
