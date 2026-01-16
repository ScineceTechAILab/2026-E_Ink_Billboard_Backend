package com.stalab.e_ink_billboard_backend.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
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

            return endpoint + "/" + bucketName + "/" + fileName;
        } catch (Exception e) {
            log.error("MinIO上传异常: {}", fileName, e);
            throw new BusinessException(500,"存储服务异常");
        }
    }

    /**
     * 删除文件
     * @param url MinIO文件URL (格式: http://endpoint/bucket-name/object-name)
     */
    public void delete(String url) {
        if (StrUtil.isBlank(url)) {
            log.warn("删除文件URL为空，跳过");
            return;
        }

        try {
            // 从URL中提取object名称
            // URL格式: http://endpoint/bucket-name/object-name
            String objectName = extractObjectName(url);
            if (StrUtil.isBlank(objectName)) {
                log.warn("无法从URL中提取object名称: {}", url);
                return;
            }

            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());

            log.info("成功删除MinIO文件: {}", objectName);
        } catch (Exception e) {
            log.error("删除MinIO文件失败: {}", url, e);
            // 不抛出异常，允许继续执行（文件可能已被删除或不存在）
        }
    }

    /**
     * 从MinIO URL中提取object名称
     * @param url MinIO文件URL
     * @return object名称
     */
    private String extractObjectName(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        // URL格式: http://endpoint/bucket-name/object-name
        // 或者: endpoint/bucket-name/object-name
        String prefix = endpoint + "/" + bucketName + "/";
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }

        // 如果URL包含bucket-name/，尝试提取后面的部分
        int bucketIndex = url.indexOf(bucketName + "/");
        if (bucketIndex >= 0) {
            return url.substring(bucketIndex + bucketName.length() + 1);
        }

        return null;
    }

    // 生成唯一文件名: uuid.png
    private String generateFileName(String originalName) {
        String suffix = FileUtil.getSuffix(originalName);
        return UUID.fastUUID().toString(true) + "." + (StrUtil.isBlank(suffix) ? "png" : suffix);
    }
}
