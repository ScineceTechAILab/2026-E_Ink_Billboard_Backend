package com.stalab.e_ink_billboard_backend.model.dto;

import lombok.Builder;
import lombok.Data;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * 视频处理结果 DTO
 */
@Data
@Builder
public class VideoProcessResult {
    /**
     * 处理后的 BIN 文件流
     */
    private ByteArrayInputStream binStream;

    /**
     * 用于审核的采样帧列表
     */
    private List<BufferedImage> sampleFrames;
}
