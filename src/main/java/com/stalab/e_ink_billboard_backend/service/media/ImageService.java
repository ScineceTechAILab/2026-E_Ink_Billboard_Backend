package com.stalab.e_ink_billboard_backend.service.media;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.enums.UserRole;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import com.stalab.e_ink_billboard_backend.common.util.ImageUtils;
import com.stalab.e_ink_billboard_backend.mapper.ImageMapper;
import com.stalab.e_ink_billboard_backend.mapper.UserMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Image;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import com.stalab.e_ink_billboard_backend.model.vo.ImageUploadVO;
import com.stalab.e_ink_billboard_backend.model.vo.ImageVO;
import com.stalab.e_ink_billboard_backend.model.vo.PageResult;
import com.stalab.e_ink_billboard_backend.service.storage.MinioService;
import com.stalab.e_ink_billboard_backend.service.wx.WeChatContentSecurityService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ImageService {

    private final ImageMapper imageMapper;
    private final UserMapper userMapper;
    private final MinioService minioService;
    private final ImageUtils imageUtils;
    private final WeChatContentSecurityService weChatContentSecurityService;

    @Value("${upload.daily-limit.image:20}")
    private int imageDailyLimit;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public ImageService(ImageMapper imageMapper, UserMapper userMapper, MinioService minioService, ImageUtils imageUtils, WeChatContentSecurityService weChatContentSecurityService) {
        this.imageMapper = imageMapper;
        this.userMapper = userMapper;
        this.minioService = minioService;
        this.imageUtils = imageUtils;
        this.weChatContentSecurityService = weChatContentSecurityService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ImageUploadVO uploadAndProcess(MultipartFile file, Long userId) {
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 0. 检查用户权限
        checkUserQuota(user);

        boolean isAdmin = UserRole.ADMIN.getCode().equals(user.getRole());

        // ★★★ 核心审核逻辑：如果是游客，必须先通过微信内容审核 ★★★
        if (!isAdmin) {
            log.info("游客上传图片，开始进行内容审核... UserId: {}", userId);
            boolean safe = weChatContentSecurityService.checkImage(file);
            if (!safe) {
                log.warn("图片审核不通过，拒绝上传 UserId: {}", userId);
                throw new BusinessException("图片包含违规内容，上传失败");
            }
            log.info("图片内容审核通过");
        }

        try {
            // 1. 计算 MD5 实现秒传
            String md5 = SecureUtil.md5(file.getInputStream());
            Image existImage = imageMapper.selectOne(new LambdaQueryWrapper<Image>()
                    .eq(Image::getMd5, md5)
                    .last("LIMIT 1"));

            if (existImage != null) {
                log.info("图片秒传触发: {}", md5);
                // 秒传也视为审核通过（因为上面已经检查过或者是管理员，或者引用了已存在的图）
                // 这里的逻辑是：如果MD5相同，我们复用旧图的URL。
                // 如果旧图是REJECTED的怎么办？
                // 如果旧图是REJECTED，但这次用户传的文件通过了审核（或者这次是管理员），
                // 那么新记录应该是APPROVED。
                Image savedImage = saveImageRecord(userId, file, existImage.getOriginalUrl(), existImage.getProcessedUrl(), md5, AuditStatus.APPROVED);
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

            // 5. 保存数据库 (因为已经通过审核，直接设置为 APPROVED)
            Image savedImage = saveImageRecord(userId, file, originalUrl, processedUrl, md5, AuditStatus.APPROVED);

            // 增加用户上传计数
            incrementUserQuota(user);

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
    private Image saveImageRecord(Long userId, MultipartFile file, String originalUrl, String processedUrl, String md5, AuditStatus status) {
        Image image = new Image();
        image.setUserId(userId);
        image.setFileName(file.getOriginalFilename());
        image.setFileSize(file.getSize());
        image.setOriginalUrl(originalUrl);
        image.setProcessedUrl(processedUrl); // 这里的图给ESP32用
        image.setMd5(md5);
        image.setAuditStatus(status); // 直接使用传入的状态

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

    /**
     * 检查用户今日上传配额
     */
    private void checkUserQuota(User user) {
        // 管理员不限制
        if (UserRole.ADMIN.getCode().equals(user.getRole())) {
            return;
        }

        String date = LocalDate.now().toString();
        String key = String.format("upload:daily:image:%d:%s", user.getId(), date);

        Object countObj = redisTemplate.opsForValue().get(key);
        int count = countObj == null ? 0 : Integer.parseInt(countObj.toString());

        if (count >= imageDailyLimit) {
            log.warn("用户今日图片上传已达上限: userId={}, count={}, limit={}", user.getId(), count, imageDailyLimit);
            throw new BusinessException("今日图片上传已达上限(" + imageDailyLimit + "张)，请明天再试");
        }
    }

    /**
     * 增加用户今日上传计数
     */
    private void incrementUserQuota(User user) {
        // 管理员不限制，不计数（或者也可以计数但不限制）
        if (UserRole.ADMIN.getCode().equals(user.getRole())) {
            return;
        }

        String date = LocalDate.now().toString();
        String key = String.format("upload:daily:image:%d:%s", user.getId(), date);

        Long count = redisTemplate.opsForValue().increment(key);
        // 如果是第一次计数，设置过期时间（24小时后，或者到明天0点）
        // 这里简单设置为24小时
        if (count != null && count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.DAYS);
        }
        log.info("用户今日图片上传计数增加: userId={}, current={}", user.getId(), count);
    }
}
