package com.stalab.e_ink_billboard_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.mapper.ImageMapper;
import com.stalab.e_ink_billboard_backend.mapper.VideoMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Image;
import com.stalab.e_ink_billboard_backend.mapper.po.Video;
import com.stalab.e_ink_billboard_backend.model.vo.StatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public AdminService(ImageMapper imageMapper, VideoMapper videoMapper) {
        this.imageMapper = imageMapper;
        this.videoMapper = videoMapper;
    }

    /**
     * 获取统计数据
     * @return 统计数据
     */
    public StatsVO getStats() {
        // 1. 统计在线设备数（暂时返回0，待设备功能实现后补充）
        int onlineDevices = 0;

        // 2. 统计待审核内容数量
        // Image中auditStatus为PENDING的数量
        long pendingImages = imageMapper.selectCount(
                new LambdaQueryWrapper<Image>()
                        .eq(Image::getAuditStatus, AuditStatus.PENDING)
        );

        // Video中auditStatus为"PENDING"的数量
        long pendingVideos = videoMapper.selectCount(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getAuditStatus, "PENDING")
        );

        int pendingAudits = (int) (pendingImages + pendingVideos);

        // 3. 统计已通过内容数量
        // Image中auditStatus为APPROVED的数量
        long approvedImages = imageMapper.selectCount(
                new LambdaQueryWrapper<Image>()
                        .eq(Image::getAuditStatus, AuditStatus.APPROVED)
        );

        // Video中auditStatus为"PASSED"的数量
        long approvedVideos = videoMapper.selectCount(
                new LambdaQueryWrapper<Video>()
                        .eq(Video::getAuditStatus, "PASSED")
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
}
