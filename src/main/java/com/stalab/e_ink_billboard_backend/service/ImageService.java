package com.stalab.e_ink_billboard_backend.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import com.stalab.e_ink_billboard_backend.common.util.ImageUtils;
import com.stalab.e_ink_billboard_backend.mapper.ImageMapper;
import com.stalab.e_ink_billboard_backend.mapper.UserMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Image;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import com.stalab.e_ink_billboard_backend.model.vo.ImageUploadVO;
import com.stalab.e_ink_billboard_backend.model.vo.ImageVO;
import com.stalab.e_ink_billboard_backend.model.vo.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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
    public ImageUploadVO uploadAndProcess(MultipartFile file, Long userId) {
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
                Image savedImage = saveImageRecord(userId, file, existImage.getOriginalUrl(), existImage.getProcessedUrl(), md5);
                return ImageUploadVO.builder()
                        .id(savedImage.getId())
                        .url(existImage.getOriginalUrl())
                        .build();
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
            Image savedImage = saveImageRecord(userId, file, originalUrl, processedUrl, md5);

            return ImageUploadVO.builder()
                    .id(savedImage.getId())
                    .url(originalUrl)
                    .build();

        } catch (IOException e) {
            log.error("图片处理流异常", e);
            throw new BusinessException("图片解析失败");
        }
    }

    /**
     * 保存图片记录
     * @return 保存后的Image对象（包含ID）
     */
    private Image saveImageRecord(Long userId, MultipartFile file, String originalUrl, String processedUrl, String md5) {
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
        return image; // 返回保存后的对象，包含自动生成的ID
    }

    /**
     * 删除图片
     * @param imageId 图片ID
     * @param userId 当前用户ID
     * @param userRole 当前用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteImage(Long imageId, Long userId, String userRole) {
        // 1. 查询图片是否存在
        Image image = imageMapper.selectById(imageId);
        if (image == null) {
            throw new BusinessException("图片不存在");
        }

        // 2. 权限检查：只有管理员或上传者可以删除
        if (!"ADMIN".equals(userRole) && !image.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此图片");
        }

        // 3. 删除MinIO中的文件
        try {
            if (StrUtil.isNotBlank(image.getOriginalUrl())) {
                minioService.delete(image.getOriginalUrl());
            }
            if (StrUtil.isNotBlank(image.getProcessedUrl())) {
                minioService.delete(image.getProcessedUrl());
            }
        } catch (Exception e) {
            log.error("删除MinIO文件失败，继续删除数据库记录", e);
            // 即使MinIO删除失败，也继续删除数据库记录
        }

        // 4. 删除数据库记录
        imageMapper.deleteById(imageId);
        log.info("成功删除图片: imageId={}, userId={}", imageId, userId);
    }

    /**
     * 查询图片列表（分页）
     * @param current 当前页码（从1开始）
     * @param size 每页大小
     * @param userId 用户ID（可选，如果提供则只查询该用户的图片）
     * @param auditStatus 审核状态（可选）
     * @return 分页结果
     */
    public PageResult<ImageVO> listImages(Long current, Long size, Long userId, AuditStatus auditStatus) {
        // 默认值
        if (current == null || current < 1) {
            current = 1L;
        }
        if (size == null || size < 1) {
            size = 10L;
        }
        // 限制每页最大数量
        if (size > 100) {
            size = 100L;
        }

        // 构建查询条件
        LambdaQueryWrapper<Image> queryWrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq(Image::getUserId, userId);
        }
        if (auditStatus != null) {
            queryWrapper.eq(Image::getAuditStatus, auditStatus);
        }
        // 按创建时间倒序
        queryWrapper.orderByDesc(Image::getCreateTime);

        // 分页查询
        Page<Image> page = new Page<>(current, size);
        IPage<Image> pageResult = imageMapper.selectPage(page, queryWrapper);

        // 转换为VO
        List<ImageVO> voList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 构建分页结果
        return PageResult.<ImageVO>builder()
                .records(voList)
                .total(pageResult.getTotal())
                .current(pageResult.getCurrent())
                .size(pageResult.getSize())
                .pages(pageResult.getPages())
                .build();
    }

    /**
     * 将Image实体转换为ImageVO
     */
    private ImageVO convertToVO(Image image) {
        return ImageVO.builder()
                .id(image.getId())
                .userId(image.getUserId())
                .fileName(image.getFileName())
                .fileSize(image.getFileSize())
                .originalUrl(image.getOriginalUrl())
                .processedUrl(image.getProcessedUrl())
                .auditStatus(image.getAuditStatus())
                .auditReason(image.getAuditReason())
                .createTime(image.getCreateTime())
                .build();
    }

    private void checkUserQuota(Long userId) {
        //TODO: 实现防白嫖逻辑
    }
}
