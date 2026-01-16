package com.stalab.e_ink_billboard_backend.service;


import com.stalab.e_ink_billboard_backend.common.util.VideoUtils;
import com.stalab.e_ink_billboard_backend.mapper.VideoMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Video;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

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

    public VideoAsyncService(VideoMapper videoMapper, MinioService minioService, VideoUtils videoUtils) {
        this.videoMapper = videoMapper;
        this.minioService = minioService;
        this.videoUtils = videoUtils;
    }

    @Async("videoExecutor") // 使用我们配置的线程池
    public void processVideoAsync(Long videoId, byte[] videoData) {
        log.info("开始异步处理视频 VideoID: {}", videoId);
        Video updateEntity = new Video();
        updateEntity.setId(videoId);

        try {
            // 1. 转码处理 (耗时操作)
            InputStream inputStream = new ByteArrayInputStream(videoData);
            ByteArrayInputStream binStream = videoUtils.processVideo(inputStream);

            // 2. 上传 BIN
            String binFileName = "processed_" + System.currentTimeMillis() + ".bin";
            String processedUrl = minioService.uploadStream(binStream, binFileName, "application/octet-stream");

            // 3. 更新成功状态
            updateEntity.setProcessedUrl(processedUrl);
            updateEntity.setProcessingStatus("SUCCESS");
            videoMapper.updateById(updateEntity);

            log.info("视频处理成功 VideoID: {}", videoId);

        } catch (Exception e) {
            log.error("视频异步处理失败 VideoID: {}", videoId, e);
            // 4. 更新失败状态
            updateEntity.setProcessingStatus("FAILED");
            updateEntity.setFailReason(e.getMessage());
            videoMapper.updateById(updateEntity);
        }
    }
}
