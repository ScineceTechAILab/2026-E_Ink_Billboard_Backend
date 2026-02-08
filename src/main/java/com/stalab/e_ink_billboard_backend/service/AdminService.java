package com.stalab.e_ink_billboard_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.enums.ContentType;
import com.stalab.e_ink_billboard_backend.common.enums.DeviceStatus;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import com.stalab.e_ink_billboard_backend.mapper.DeviceMapper;
import com.stalab.e_ink_billboard_backend.mapper.ImageMapper;
import com.stalab.e_ink_billboard_backend.mapper.UserMapper;
import com.stalab.e_ink_billboard_backend.mapper.VideoMapper;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 管理员服务
 * @Version: 1.0
 */
@Slf4j
@Service
public class AdminService {

    private final ImageMapper imageMapper;
    private final VideoMapper videoMapper;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;

    public AdminService(ImageMapper imageMapper, VideoMapper videoMapper, DeviceMapper deviceMapper, UserMapper userMapper) {
        this.imageMapper = imageMapper;
        this.videoMapper = videoMapper;
        this.deviceMapper = deviceMapper;
        this.userMapper = userMapper;
    }

    /**
     * 获取统计数据
     * @return 统计数据
     */
    public StatsVO getStats() {
        // 1. 统计在线设备数
        long onlineDevicesCount = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getStatus, DeviceStatus.ONLINE)
        );
        int onlineDevices = (int) onlineDevicesCount;

        // 2. 统计待审核内容数量
        // Image中auditStatus为PENDING的数量
        long pendingImages = imageMapper.selectCount(
                new LambdaQueryWrapper<Image>()
                        .eq(Image::getAuditStatus, AuditStatus.PENDING)
        );

        // Video中auditStatus为"PENDING"的数量
        long pendingVideos = videoMapper.selectCount(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getAuditStatus, AuditStatus.PENDING.name())
        );

        int pendingAudits = (int) (pendingImages + pendingVideos);

        // 3. 统计已通过内容数量
        // Image中auditStatus为APPROVED的数量
        long approvedImages = imageMapper.selectCount(
                new LambdaQueryWrapper<Image>()
                        .eq(Image::getAuditStatus, AuditStatus.APPROVED)
        );

        // Video中auditStatus为APPROVED的数量
        long approvedVideos = videoMapper.selectCount(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getAuditStatus, AuditStatus.APPROVED.name())
        );

        int approvedContent = (int) (approvedImages + approvedVideos);

        // 构建返回对象（同时支持两种字段名格式）
        return StatsVO.builder()
                .onlineDevices(onlineDevices)
                .pendingAudits(pendingAudits)
                .approvedContent(approvedContent)
                // 兼容字段名
                .online(onlineDevices)
                .pending(pendingAudits)
                .approved(approvedContent)
                .build();
    }

    /**
     * 获取待审核列表（包含图片和视频）
     *
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    public PageResult<AuditItemVO> getPendingAuditList(Long current, Long size) {
        if (current == null || current < 1) current = 1L;
        if (size == null || size < 1) size = 10L;

        List<AuditItemVO> allPendingItems = new ArrayList<>();

        // 1. 查询所有待审核图片
        List<Image> pendingImages = imageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Image>()
                        .eq(Image::getAuditStatus, AuditStatus.PENDING)
        );

        for (Image img : pendingImages) {
            allPendingItems.add(AuditItemVO.builder()
                    .id(img.getId())
                    .contentType(ContentType.IMAGE)
                    .userId(img.getUserId())
                    .originalUrl(img.getOriginalUrl()) // 管理员看原图
                    .fileName(img.getFileName())
                    .auditStatus(img.getAuditStatus())
                    .auditReason(img.getAuditReason())
                    .createTime(img.getCreateTime())
                    .build());
        }

        // 2. 查询所有待审核视频
        List<Video> pendingVideos = videoMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Video>()
                        .eq(Video::getAuditStatus, AuditStatus.PENDING.name())
        );

        for (Video video : pendingVideos) {
            allPendingItems.add(AuditItemVO.builder()
                    .id(video.getId())
                    .contentType(ContentType.VIDEO)
                    .userId(video.getUserId())
                    .originalUrl(video.getOriginalUrl())
                    .fileName(video.getFileName())
                    .auditStatus(AuditStatus.PENDING)
                    .auditReason(null) // 视频暂时没有auditReason字段，或者可以加
                    .createTime(video.getCreateTime())
                    .build());
        }

        // 3. 填充用户信息（批量查询优化，或者循环查询）
        // 这里简单循环查询，后续可优化
        for (AuditItemVO item : allPendingItems) {
            User user = userMapper.selectById(item.getUserId());
            if (user != null) {
                item.setUserName(user.getNickname());
            }
        }

        // 4. 排序（按时间倒序，最早提交的在最后？或者最先提交的在最前？通常审核按提交顺序，先提交先审）
        // 这里按提交时间升序（先提交的先显示）
        allPendingItems.sort(Comparator.comparing(AuditItemVO::getCreateTime));

        // 5. 内存分页
        int total = allPendingItems.size();
        int start = (int) ((current - 1) * size);
        int end = Math.min(start + size.intValue(), total);

        List<AuditItemVO> pageRecords = new ArrayList<>();
        if (start < total) {
            pageRecords = allPendingItems.subList(start, end);
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
     * 审核内容
     *
     * @param dto 审核结果
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditContent(AuditResultDTO dto) {
        if (dto.getContentType() == ContentType.IMAGE) {
            Image image = imageMapper.selectById(dto.getContentId());
            if (image == null) {
                throw new BusinessException("图片不存在");
            }

            if (dto.getAuditStatus() == AuditStatus.REJECTED) {
                if (dto.getRejectReason() == null || dto.getRejectReason().trim().isEmpty()) {
                    throw new BusinessException("拒绝时必须填写原因");
                }
                image.setAuditReason(dto.getRejectReason());
            }

            image.setAuditStatus(dto.getAuditStatus());
            // 更新时间不需要手动设吗？Entity可能有自动填充，这里简单起见不设
            imageMapper.updateById(image);
            log.info("管理员审核图片: id={}, status={}, reason={}", image.getId(), dto.getAuditStatus(), dto.getRejectReason());

        } else if (dto.getContentType() == ContentType.VIDEO) {
            Video video = videoMapper.selectById(dto.getContentId());
            if (video == null) {
                throw new BusinessException("视频不存在");
            }

            if (dto.getAuditStatus() == AuditStatus.REJECTED) {
                if (dto.getRejectReason() == null || dto.getRejectReason().trim().isEmpty()) {
                    throw new BusinessException("拒绝时必须填写原因");
                }
                video.setFailReason(dto.getRejectReason()); // 视频用failReason存拒绝原因
            }

            video.setAuditStatus(dto.getAuditStatus().name());
            videoMapper.updateById(video);
            log.info("管理员审核视频: id={}, status={}, reason={}", video.getId(), dto.getAuditStatus(), dto.getRejectReason());
        }
    }
}
