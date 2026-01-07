package com.stalab.e_ink_billboard_backend.service;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import com.stalab.e_ink_billboard_backend.common.util.ImageUtils;
import com.stalab.e_ink_billboard_backend.mapper.ImageMapper;
import com.stalab.e_ink_billboard_backend.mapper.UserMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Image;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@Service
public class ImageService {

    private final ImageMapper imageMapper;
    private final UserMapper userMapper;
    private final MinioService minioService;
    private final ImageUtils imageUtils;

    public ImageService(ImageMapper imageMapper, UserMapper userMapper, MinioService minioService, ImageUtils imageUtils) {
        this.imageMapper = imageMapper;
        this.userMapper = userMapper;
        this.minioService = minioService;
        this.imageUtils = imageUtils;
    }

    @Transactional(rollbackFor = Exception.class)
    public String uploadAndProcess(MultipartFile file, Long userId) {
        // 0. 检查用户权限
        checkUserQuota(userId);

        try {
            // 1. 计算 MD5 实现秒传
            String md5 = SecureUtil.md5(file.getInputStream());
            Image existImage = imageMapper.selectOne(new LambdaQueryWrapper<Image>()
                    .eq(Image::getMd5, md5)
                    .last("LIMIT 1"));

            if (existImage != null) {
                log.info("图片秒传触发: {}", md5);
                saveImageRecord(userId, file, existImage.getOriginalUrl(), existImage.getProcessedUrl(), md5);
                return existImage.getOriginalUrl();
            }

            // 2. 上传原图 (Color)
            String originalUrl = minioService.upload(file);

            // 3. 算法处理 (Color -> Dithered BW)
            ByteArrayInputStream ditheredStream = imageUtils.processImage(file.getInputStream());

            // 4. 上传处理图 (BW)
            String processedUrl = minioService.uploadStream(ditheredStream,
                    "dithered_" + System.currentTimeMillis() + ".png",
                    "image/png");

            // 5. 保存数据库
            saveImageRecord(userId, file, originalUrl, processedUrl, md5);

            return originalUrl;

        } catch (IOException e) {
            log.error("图片处理流异常", e);
            throw new BusinessException("图片解析失败");
        }
    }

    private void saveImageRecord(Long userId, MultipartFile file, String originalUrl, String processedUrl, String md5) {
        Image image = new Image();
        image.setUserId(userId);
        image.setFileName(file.getOriginalFilename());
        image.setFileSize(file.getSize());
        image.setOriginalUrl(originalUrl);
        image.setProcessedUrl(processedUrl); // 这里的图给ESP32用
        image.setMd5(md5);

        // 鉴权逻辑：如果是管理员直接通过，游客则待审核
        User user = userMapper.selectById(userId);
        if ("ADMIN".equals(user.getRole())) {
            image.setAuditStatus(AuditStatus.APPROVED);
        } else {
            image.setAuditStatus(AuditStatus.PENDING);
        }

        imageMapper.insert(image);
    }

    private void checkUserQuota(Long userId) {
        //TODO: 实现防白嫖逻辑
    }
}
