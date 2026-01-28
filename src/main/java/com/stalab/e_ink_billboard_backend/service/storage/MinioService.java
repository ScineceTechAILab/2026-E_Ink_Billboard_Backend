package com.stalab.e_ink_billboard_backend.service.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioService {


    private final MinioClient minioClient;



    @Value("${minio.bucket-name:eink-images}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 是否使用Presigned URL（如果bucket是私有的，必须设为true）
     * 如果bucket是公开的，可以设为false直接使用永久URL
     */
    @Value("${minio.use-presigned-url:true}")
    private boolean usePresignedUrl;

    /**
     * Presigned URL的有效期（小时），默认2小时
     */
    @Value("${minio.presigned-url-expiry-hours:2}")
    private int presignedUrlExpiryHours;

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
     * 获取文件的下载URL（用于推送到ESP32）
     * 如果配置了使用Presigned URL，则动态生成；否则返回永久URL
     *
     * @param storedUrl 数据库中存储的URL（可能是永久URL或object名称）
     * @return 可用于下载的URL
     */
    public String getDownloadUrl(String storedUrl) {
        if (StrUtil.isBlank(storedUrl)) {
            throw new BusinessException(400, "文件URL为空");
        }

        // 如果不需要Presigned URL（bucket是公开的），直接返回存储的URL
        if (!usePresignedUrl) {
            return storedUrl;
        }

        // 需要生成Presigned URL
        String objectName = extractObjectName(storedUrl);
        if (StrUtil.isBlank(objectName)) {
            // 如果无法提取object名称，可能存储的就是object名称本身
            objectName = storedUrl;
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(presignedUrlExpiryHours, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成Presigned URL失败: objectName={}", objectName, e);
            throw new BusinessException(500, "生成下载链接失败");
        }
    }

    /**
     * 从MinIO URL中提取object名称
     * @param url MinIO文件URL（可能是永久URL或Presigned URL）
     * @return object名称
     */
    public String extractObjectName(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        // 先移除URL中的查询参数（Presigned URL可能包含?X-Amz-...）
        String urlWithoutQuery = url;
        int queryIndex = url.indexOf('?');
        if (queryIndex >= 0) {
            urlWithoutQuery = url.substring(0, queryIndex);
        }

        // URL格式: http://endpoint/bucket-name/object-name
        // 或者: endpoint/bucket-name/object-name
        String prefix = endpoint + "/" + bucketName + "/";
        if (urlWithoutQuery.startsWith(prefix)) {
            return urlWithoutQuery.substring(prefix.length());
        }

        // 如果URL包含bucket-name/，尝试提取后面的部分
        int bucketIndex = urlWithoutQuery.indexOf(bucketName + "/");
        if (bucketIndex >= 0) {
            String objectName = urlWithoutQuery.substring(bucketIndex + bucketName.length() + 1);
            // 如果对象名包含路径分隔符，需要URL解码
            // 但通常我们的对象名不包含特殊字符，所以直接返回
            return objectName;
        }

        // 如果以上都不匹配，可能存储的就是对象名本身（不包含路径）
        // 检查是否包含斜杠，如果不包含，可能是纯对象名
        if (!urlWithoutQuery.contains("/")) {
            return urlWithoutQuery;
        }

        return null;
    }

    // 生成唯一文件名: uuid.png
    private String generateFileName(String originalName) {
        String suffix = FileUtil.getSuffix(originalName);
        return UUID.fastUUID().toString(true) + "." + (StrUtil.isBlank(suffix) ? "png" : suffix);
    }
}
