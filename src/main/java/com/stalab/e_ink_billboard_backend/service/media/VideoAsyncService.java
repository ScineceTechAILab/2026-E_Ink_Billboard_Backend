package com.stalab.e_ink_billboard_backend.service.media;


import com.stalab.e_ink_billboard_backend.common.enums.AuditStatus;
import com.stalab.e_ink_billboard_backend.common.enums.ProcessingStatus;
import com.stalab.e_ink_billboard_backend.common.util.VideoUtils;
import com.stalab.e_ink_billboard_backend.mapper.VideoMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Video;
import com.stalab.e_ink_billboard_backend.model.dto.VideoProcessResult;
import com.stalab.e_ink_billboard_backend.service.storage.MinioService;
import com.stalab.e_ink_billboard_backend.service.wx.WeChatContentSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 异步处理视频
 * @Version: 1.0
 */

@Service
@Slf4j
public class VideoAsyncService {

    private final VideoMapper videoMapper;
    private final MinioService minioService;
    private final VideoUtils videoUtils;
    private final WeChatContentSecurityService weChatContentSecurityService;

    public VideoAsyncService(VideoMapper videoMapper, MinioService minioService, VideoUtils videoUtils, WeChatContentSecurityService weChatContentSecurityService) {
        this.videoMapper = videoMapper;
        this.minioService = minioService;
        this.videoUtils = videoUtils;
        this.weChatContentSecurityService = weChatContentSecurityService;
    }

    @Async("videoExecutor") // 使用我们配置的线程池
    public void processVideoAsync(Long videoId, byte[] videoData) {
        log.info("开始异步处理视频 VideoID: {}", videoId);

        // 查询视频当前状态 (判断是否需要审核)
        Video currentVideo = videoMapper.selectById(videoId);
        if (currentVideo == null) {
            log.error("视频不存在 VideoID: {}", videoId);
            return;
        }

        Video updateEntity = new Video();
        updateEntity.setId(videoId);

        try {
            // 1. 转码处理 (耗时操作)
            InputStream inputStream = new ByteArrayInputStream(videoData);
            // 修改为返回 Result 对象
            VideoProcessResult result = videoUtils.processVideo(inputStream);

            // 2. 内容审核 (如果当前状态是 PENDING，说明是游客上传，需要审核)
            if (AuditStatus.PENDING.name().equals(currentVideo.getAuditStatus())) {
                log.info("开始对视频进行内容审核 VideoID: {}", videoId);
                List<BufferedImage> samples = result.getSampleFrames();
                if (samples != null && !samples.isEmpty()) {
                    for (BufferedImage frame : samples) {
                        boolean safe = weChatContentSecurityService.checkImage(frame);
                        if (!safe) {
                            // 审核不通过
                            log.warn("视频审核未通过 VideoID: {}", videoId);
                            updateEntity.setAuditStatus(AuditStatus.REJECTED.name());
                            updateEntity.setProcessingStatus(ProcessingStatus.FAILED.name());
                            updateEntity.setFailReason("视频包含违规内容，审核未通过");
                            videoMapper.updateById(updateEntity);
                            return; // 终止后续流程
                        }
                    }
                }
                // 审核通过
                log.info("视频审核通过 VideoID: {}", videoId);
                updateEntity.setAuditStatus(AuditStatus.APPROVED.name());
            }

            // 3. 上传 BIN
            String binFileName = "processed_" + System.currentTimeMillis() + ".bin";
            String processedUrl = minioService.uploadStream(result.getBinStream(), binFileName, "application/octet-stream");

            // 4. 更新成功状态
            updateEntity.setProcessedUrl(processedUrl);
            updateEntity.setProcessingStatus(ProcessingStatus.SUCCESS.name());
            videoMapper.updateById(updateEntity);

            log.info("视频处理成功 VideoID: {}", videoId);

        } catch (Throwable e) {
            log.error("视频异步处理失败 VideoID: {}", videoId, e);
            // 5. 更新失败状态
            updateEntity.setProcessingStatus(ProcessingStatus.FAILED.name());
            updateEntity.setFailReason(e.getMessage());
            videoMapper.updateById(updateEntity);
        }
    }
}
