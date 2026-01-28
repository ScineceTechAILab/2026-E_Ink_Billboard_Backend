package com.stalab.e_ink_billboard_backend.service.media;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.enums.ProcessingStatus;
import com.stalab.e_ink_billboard_backend.common.enums.UserRole;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import com.stalab.e_ink_billboard_backend.mapper.UserMapper;
import com.stalab.e_ink_billboard_backend.mapper.VideoMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import com.stalab.e_ink_billboard_backend.mapper.po.Video;
import com.stalab.e_ink_billboard_backend.model.vo.PageResult;
import com.stalab.e_ink_billboard_backend.model.vo.VideoVO;
import com.stalab.e_ink_billboard_backend.service.storage.MinioService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Service
public class VideoService {


    private final VideoMapper videoMapper;

    private final UserMapper userMapper;

    private final MinioService minioService;

    private final VideoAsyncService videoAsyncService;

    @Value("${upload.daily-limit.video:5}")
    private int videoDailyLimit;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public VideoService(VideoMapper videoMapper, UserMapper userMapper, MinioService minioService, VideoAsyncService videoAsyncService) {
        this.videoMapper = videoMapper;
        this.userMapper = userMapper;
        this.minioService = minioService;
        this.videoAsyncService = videoAsyncService;
    }

    // ================= 阶段一：同步上传 (极速返回) =================

    @Transactional(rollbackFor = Exception.class)
    public Long initUpload(MultipartFile file, Long userId) {
        // 1. 基础校验
        if (file.isEmpty()) throw new BusinessException("文件为空");
        if (file.getSize() > 50 * 1024 * 1024) throw new BusinessException("视频超过50MB");

        // 检查用户权限
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        checkUserQuota(user);

        String originalFileName = file.getOriginalFilename();

        try {
            // 2. 上传原视频 (必须同步做，否则文件流会关闭)
            String originalUrl = minioService.upload(file);

            // 3. 预先保存数据库记录 (状态: PROCESSING)
            Video video = new Video();
            video.setUserId(userId);
            video.setFileName(originalFileName);
            video.setFileSize(file.getSize());
            video.setOriginalUrl(originalUrl);
            video.setProcessingStatus(ProcessingStatus.PROCESSING.name()); // <--- 标记为处理中

            // 审核状态初始化
            video.setAuditStatus(UserRole.ADMIN.getCode().equals(user.getRole()) ? AuditStatus.APPROVED.name() : AuditStatus.PENDING.name());

            videoMapper.insert(video);

            // 增加计数
            incrementUserQuota(user);

            // 4. ★★★ 启动异步处理 ★★★
            byte[] videoBytes = file.getBytes();

            videoAsyncService.processVideoAsync(video.getId(), videoBytes);

            return video.getId(); // 立刻返回 ID 给前端

        } catch (Exception e) {
            log.error("初始化上传失败", e);
            throw new BusinessException("上传失败");
        }
    }

    /**
     * 删除视频
     * @param videoId 视频ID
     * @param userId 当前用户ID
     * @param userRole 当前用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteVideo(Long videoId, Long userId, String userRole) {
        // 1. 查询视频是否存在
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }

        // 2. 权限检查：只有管理员或上传者可以删除
        if (!UserRole.ADMIN.getCode().equals(userRole) && !video.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此视频");
        }

        // 3. 删除MinIO中的文件
        try {
            if (StrUtil.isNotBlank(video.getOriginalUrl())) {
                minioService.delete(video.getOriginalUrl());
            }
            if (StrUtil.isNotBlank(video.getProcessedUrl())) {
                minioService.delete(video.getProcessedUrl());
            }
        } catch (Exception e) {
            log.error("删除MinIO文件失败，继续删除数据库记录", e);
            // 即使MinIO删除失败，也继续删除数据库记录
        }

        // 4. 删除数据库记录
        videoMapper.deleteById(videoId);
        log.info("成功删除视频: videoId={}, userId={}", videoId, userId);
    }

    /**
     * 查询视频列表（分页）
     * @param current 当前页码（从1开始）
     * @param size 每页大小
     * @param userId 用户ID（可选，如果提供则只查询该用户的视频）
     * @param auditStatus 审核状态（可选，如"PENDING", "PASSED", "REJECTED"）
     * @param processingStatus 处理状态（可选，如"PROCESSING", "SUCCESS", "FAILED"）
     * @return 分页结果
     */
    public PageResult<VideoVO> listVideos(Long current, Long size, Long userId, String auditStatus, String processingStatus) {
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
        LambdaQueryWrapper<Video> queryWrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq(Video::getUserId, userId);
        }
        if (StrUtil.isNotBlank(auditStatus)) {
            queryWrapper.eq(Video::getAuditStatus, auditStatus);
        }
        if (StrUtil.isNotBlank(processingStatus)) {
            queryWrapper.eq(Video::getProcessingStatus, processingStatus);
        }
        // 按创建时间倒序
        queryWrapper.orderByDesc(Video::getCreateTime);

        // 分页查询
        Page<Video> page = new Page<>(current, size);
        IPage<Video> pageResult = videoMapper.selectPage(page, queryWrapper);

        // 转换为VO
        List<VideoVO> voList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 构建分页结果
        return PageResult.<VideoVO>builder()
                .records(voList)
                .total(pageResult.getTotal())
                .current(pageResult.getCurrent())
                .size(pageResult.getSize())
                .pages(pageResult.getPages())
                .build();
    }

    /**
     * 将Video实体转换为VideoVO
     */
    private VideoVO convertToVO(Video video) {
        return VideoVO.builder()
                .id(video.getId())
                .userId(video.getUserId())
                .fileName(video.getFileName())
                .fileSize(video.getFileSize())
                .originalUrl(video.getOriginalUrl())
                .processedUrl(video.getProcessedUrl())
                .duration(video.getDuration())
                .frameCount(video.getFrameCount())
                .auditStatus(video.getAuditStatus())
                .processingStatus(video.getProcessingStatus())
                .failReason(video.getFailReason())
                .createTime(video.getCreateTime())
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
        String key = String.format("upload:daily:video:%d:%s", user.getId(), date);

        Object countObj = redisTemplate.opsForValue().get(key);
        int count = countObj == null ? 0 : Integer.parseInt(countObj.toString());

        if (count >= videoDailyLimit) {
            log.warn("用户今日视频上传已达上限: userId={}, count={}, limit={}", user.getId(), count, videoDailyLimit);
            throw new BusinessException("今日视频上传已达上限(" + videoDailyLimit + "个)，请明天再试");
        }
    }

    /**
     * 增加用户今日上传计数
     */
    private void incrementUserQuota(User user) {
        // 管理员不限制
        if (UserRole.ADMIN.getCode().equals(user.getRole())) {
            return;
        }

        String date = LocalDate.now().toString();
        String key = String.format("upload:daily:video:%d:%s", user.getId(), date);

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.DAYS);
        }
        log.info("用户今日视频上传计数增加: userId={}, current={}", user.getId(), count);
    }
}
