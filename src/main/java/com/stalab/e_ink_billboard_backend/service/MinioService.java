package com.stalab.e_ink_billboard_backend.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
public class MinioService {


    private final MinioClient minioClient;



    @Value("${minio.bucket-name:eink-images}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }
    /**
     * 上传 MultipartFile (用户上传的原图)
     */
    public String upload(MultipartFile file) {
        String fileName = generateFileName(file.getOriginalFilename());
        try {
            return uploadStream(file.getInputStream(), fileName, file.getContentType());
        } catch (Exception e) {
            log.error("原图上传失败", e);
            throw new BusinessException(500,"文件上传失败");
        }
    }

    /**
     * 上传 InputStream (算法处理后的抖动图)
     */
    public String uploadStream(InputStream stream, String fileName, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(stream, -1, 10485760) // -1表示未知大小, 10M分片
                    .contentType(contentType)
                    .build());

            // 返回可访问的 URL (假设桶是 Public 的)
            return endpoint + "/" + bucketName + "/" + fileName;
        } catch (Exception e) {
            log.error("MinIO上传异常: {}", fileName, e);
            throw new BusinessException(500,"存储服务异常");
        }
    }

    // 生成唯一文件名: uuid.png
    private String generateFileName(String originalName) {
        String suffix = FileUtil.getSuffix(originalName);
        return UUID.fastUUID().toString(true) + "." + (StrUtil.isBlank(suffix) ? "png" : suffix);
    }
}
