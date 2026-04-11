package com.stalab.e_ink_billboard_backend.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.internal-endpoint:${minio.endpoint}}")
    private String internalEndpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        // 优先使用内部地址连接 MinIO（用于容器间通信）
        // 如果没有配置内部地址，则使用外部地址
        String connectEndpoint = internalEndpoint != null && !internalEndpoint.isEmpty()
                ? internalEndpoint
                : endpoint;

        return MinioClient.builder()
                .endpoint(connectEndpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
