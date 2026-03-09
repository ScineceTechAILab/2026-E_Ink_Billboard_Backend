package com.stalab.e_ink_billboard_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import com.stalab.e_ink_billboard_backend.common.enums.DeviceStatus;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import com.stalab.e_ink_billboard_backend.mapper.AuditLogMapper;
import com.stalab.e_ink_billboard_backend.mapper.DeviceMapper;
import com.stalab.e_ink_billboard_backend.mapper.ImageMapper;
import com.stalab.e_ink_billboard_backend.mapper.UserMapper;
import com.stalab.e_ink_billboard_backend.mapper.VideoMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.AuditLog;
import com.stalab.e_ink_billboard_backend.mapper.po.Device;
import com.stalab.e_ink_billboard_backend.mapper.po.Image;
import com.stalab.e_ink_billboard_backend.mapper.po.User;
import com.stalab.e_ink_billboard_backend.mapper.po.Video;
import com.stalab.e_ink_billboard_backend.model.dto.AuditResultDTO;
import com.stalab.e_ink_billboard_backend.model.vo.AuditItemVO;
import com.stalab.e_ink_billboard_backend.model.vo.PageResult;
import com.stalab.e_ink_billboard_backend.model.vo.StatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 管理员服务
 */
@Slf4j
@Service
public class AdminService {

    private final ImageMapper imageMapper;
    private final VideoMapper videoMapper;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;
    private final AuditLogMapper auditLogMapper;

    public AdminService(ImageMapper imageMapper, VideoMapper videoMapper, DeviceMapper deviceMapper,
                        UserMapper userMapper, AuditLogMapper auditLogMapper) {
        this.imageMapper = imageMapper;
        this.videoMapper = videoMapper;
        this.deviceMapper = deviceMapper;
        this.userMapper = userMapper;
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 获取统计数据
     * @return 统计数据
     */
    public StatsVO getStats() {
        long onlineDevicesCount = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getStatus, DeviceStatus.ONLINE)
        );
        int onlineDevices = (int) onlineDevicesCount;

        long pendingImages = imageMapper.selectCount(
                new LambdaQueryWrapper<Image>()
                        .eq(Image::getAuditStatus, AuditStatus.PENDING)
        );

        long pendingVideos = videoMapper.selectCount(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getAuditStatus, AuditStatus.PENDING.name())
        );

        int pendingAudits = (int) (pendingImages + pendingVideos);

        long approvedImages = imageMapper.selectCount(
                new LambdaQueryWrapper<Image>()
                        .eq(Image::getAuditStatus, AuditStatus.APPROVED)
        );

        long approvedVideos = videoMapper.selectCount(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getAuditStatus, AuditStatus.APPROVED.name())
        );

        int approvedContent = (int) (approvedImages + approvedVideos);

        return StatsVO.builder()
                .onlineDevices(onlineDevices)
                .pendingAudits(pendingAudits)
                .approvedContent(approvedContent)
                .online(onlineDevices)
                .pending(pendingAudits)
                .approved(approvedContent)
                .build();
    }

    /**
     * 获取审核列表（支持状态筛选）
     *
     * @param current 当前页
     * @param size    每页大小
     * @param auditStatus 审核状态筛选
     * @param contentType 内容类型筛选
     * @return 分页结果
     */
    public PageResult<AuditItemVO> getAuditList(Long current, Long size,
                                               AuditStatus auditStatus,
                                               ContentType contentType) {
        if (current == null || current < 1) current = 1L;
        if (size == null || size < 1) size = 10L;

        List<AuditItemVO> allItems = new ArrayList<>();

        if (contentType == null || contentType == ContentType.IMAGE) {
            LambdaQueryWrapper<Image> imgWrapper = new LambdaQueryWrapper<>();
            if (auditStatus != null) {
                imgWrapper.eq(Image::getAuditStatus, auditStatus);
            }
            List<Image> images = imageMapper.selectList(imgWrapper);

            for (Image img : images) {
                allItems.add(AuditItemVO.builder()
                        .id(img.getId())
                        .contentType(ContentType.IMAGE)
                        .userId(img.getUserId())
                        .originalUrl(img.getOriginalUrl())
                        .fileName(img.getFileName())
                        .auditStatus(img.getAuditStatus())
                        .auditReason(img.getAuditReason())
                        .createTime(img.getCreateTime())
                        .build());
            }
        }

        if (contentType == null || contentType == ContentType.VIDEO) {
            LambdaQueryWrapper<Video> videoWrapper = new LambdaQueryWrapper<>();
            if (auditStatus != null) {
                videoWrapper.eq(Video::getAuditStatus, auditStatus.name());
            }
            List<Video> videos = videoMapper.selectList(videoWrapper);

            for (Video video : videos) {
                allItems.add(AuditItemVO.builder()
                        .id(video.getId())
                        .contentType(ContentType.VIDEO)
                        .userId(video.getUserId())
                        .originalUrl(video.getOriginalUrl())
                        .fileName(video.getFileName())
                        .auditStatus(AuditStatus.valueOf(video.getAuditStatus()))
                        .auditReason(video.getFailReason())
                        .createTime(video.getCreateTime())
                        .build());
            }
        }

        for (AuditItemVO item : allItems) {
            User user = userMapper.selectById(item.getUserId());
            if (user != null) {
                item.setUserName(user.getNickname());
            }
        }

        allItems.sort(Comparator.comparing(AuditItemVO::getCreateTime).reversed());

        int total = allItems.size();
        int start = (int) ((current - 1) * size);
        int end = Math.min(start + size.intValue(), total);

        List<AuditItemVO> pageRecords = new ArrayList<>();
        if (start < total) {
            pageRecords = allItems.subList(start, end);
        }

        return PageResult.<AuditItemVO>builder()
                .records(pageRecords)
                .total((long) total)
                .current(current)
                .size(size)
                .pages((long) Math.ceil((double) total / size))
                .build();
    }

    /**
     * 获取待审核列表
     */
    public PageResult<AuditItemVO> getPendingAuditList(Long current, Long size) {
        return getAuditList(current, size, AuditStatus.PENDING, null);
    }

    /**
     * 审核内容
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditContent(AuditResultDTO dto, Long auditorId, String auditorName) {
        AuditStatus beforeStatus = null;

        if (dto.getContentType() == ContentType.IMAGE) {
            Image image = imageMapper.selectById(dto.getContentId());
            if (image == null) {
                throw new BusinessException("图片不存在");
            }
            beforeStatus = image.getAuditStatus();

            if (dto.getAuditStatus() == AuditStatus.REJECTED) {
                if (dto.getRejectReason() == null || dto.getRejectReason().trim().isEmpty()) {
                    throw new BusinessException("拒绝时必须填写原因");
                }
                image.setAuditReason(dto.getRejectReason());
            }

            image.setAuditStatus(dto.getAuditStatus());
            imageMapper.updateById(image);
            log.info("管理员审核图片: id={}, status={}, reason={}", image.getId(), dto.getAuditStatus(), dto.getRejectReason());

        } else if (dto.getContentType() == ContentType.VIDEO) {
            Video video = videoMapper.selectById(dto.getContentId());
            if (video == null) {
                throw new BusinessException("视频不存在");
            }
            beforeStatus = AuditStatus.valueOf(video.getAuditStatus());

            if (dto.getAuditStatus() == AuditStatus.REJECTED) {
                if (dto.getRejectReason() == null || dto.getRejectReason().trim().isEmpty()) {
                    throw new BusinessException("拒绝时必须填写原因");
                }
                video.setFailReason(dto.getRejectReason());
            }

            video.setAuditStatus(dto.getAuditStatus().name());
            videoMapper.updateById(video);
            log.info("管理员审核视频: id={}, status={}, reason={}", video.getId(), dto.getAuditStatus(), dto.getRejectReason());
        }

        saveAuditLog(dto.getContentId(), dto.getContentType(), beforeStatus,
                dto.getAuditStatus(), auditorId, auditorName, dto.getRejectReason(), "AUDIT");
    }

    /**
     * 重新审核（将已审核内容重置为待审核）
     */
    @Transactional(rollbackFor = Exception.class)
    public void reAudit(Long contentId, ContentType contentType, Long auditorId, String auditorName) {
        AuditStatus beforeStatus = null;

        if (contentType == ContentType.IMAGE) {
            Image image = imageMapper.selectById(contentId);
            if (image == null) {
                throw new BusinessException("图片不存在");
            }
            beforeStatus = image.getAuditStatus();

            if (beforeStatus == AuditStatus.PENDING) {
                throw new BusinessException("该内容已是待审核状态");
            }

            image.setAuditStatus(AuditStatus.PENDING);
            image.setAuditReason(null);
            imageMapper.updateById(image);
            log.info("管理员重新审核图片: id={}, previousStatus={}", image.getId(), beforeStatus);

        } else if (contentType == ContentType.VIDEO) {
            Video video = videoMapper.selectById(contentId);
            if (video == null) {
                throw new BusinessException("视频不存在");
            }
            beforeStatus = AuditStatus.valueOf(video.getAuditStatus());

            if (beforeStatus == AuditStatus.PENDING) {
                throw new BusinessException("该内容已是待审核状态");
            }

            video.setAuditStatus(AuditStatus.PENDING.name());
            video.setFailReason(null);
            videoMapper.updateById(video);
            log.info("管理员重新审核视频: id={}, previousStatus={}", video.getId(), beforeStatus);
        }

        saveAuditLog(contentId, contentType, beforeStatus, AuditStatus.PENDING,
                auditorId, auditorName, null, "RE_AUDIT");
    }

    /**
     * 保存审核记录
     */
    private void saveAuditLog(Long contentId, ContentType contentType,
                              AuditStatus beforeStatus, AuditStatus afterStatus,
                              Long auditorId, String auditorName,
                              String auditReason, String operationType) {
        AuditLog auditLog = AuditLog.builder()
                .contentId(contentId)
                .contentType(contentType)
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .auditorId(auditorId)
                .auditorName(auditorName)
                .auditReason(auditReason)
                .operationType(operationType)
                .createTime(LocalDateTime.now())
                .build();
        auditLogMapper.insert(auditLog);
    }

    /**
     * 获取内容的审核历史记录
     */
    public List<AuditLog> getAuditHistory(Long contentId, ContentType contentType) {
        return auditLogMapper.selectList(
                new LambdaQueryWrapper<AuditLog>()
                        .eq(AuditLog::getContentId, contentId)
                        .eq(AuditLog::getContentType, contentType)
                        .orderByDesc(AuditLog::getCreateTime)
        );
    }

    /**
     * 获取所有用户列表（用于筛选）
     */
    public List<User> getUsers() {
        return userMapper.selectList(null);
    }
}
