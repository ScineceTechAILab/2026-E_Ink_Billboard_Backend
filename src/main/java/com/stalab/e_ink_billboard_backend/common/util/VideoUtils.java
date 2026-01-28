package com.stalab.e_ink_billboard_backend.common.util;

import com.stalab.e_ink_billboard_backend.model.dto.VideoProcessResult;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Component;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class VideoUtils {


    private final ImageUtils imageUtils;

    // 目标帧率
    private static final int TARGET_FPS = 5;

    public VideoUtils(ImageUtils imageUtils) {
        this.imageUtils = imageUtils;
    }

    /**
     * 处理视频：抽帧 -> 抖动 -> 打包成 BIN
     * 同时收集采样帧用于审核
     * @return VideoProcessResult (BIN流 + 采样帧)
     */
    public VideoProcessResult processVideo(InputStream inputStream) throws Exception {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream);
        grabber.start();

        // 原始帧率
        double frameRate = grabber.getFrameRate();
        // 计算采样步长：比如原视频30fps，我们要5fps，那就每隔6帧采一次
        int step = (int) Math.max(1, frameRate / TARGET_FPS);

        Java2DFrameConverter converter = new Java2DFrameConverter();
        ByteArrayOutputStream binOutputStream = new ByteArrayOutputStream();

        int frameIndex = 0;
        int processedCount = 0;
        org.bytedeco.javacv.Frame frame;

        List<BufferedImage> sampleFrames = new ArrayList<>();

        while ((frame = grabber.grabImage()) != null) {
            // 抽帧逻辑
            if (frameIndex % step == 0) {
                // 1. 转成 Java 图片对象
                BufferedImage srcImage = converter.getBufferedImage(frame);
                if (srcImage != null) {
                    // 收集采样帧 (每隔10个处理帧收集一次，即每2秒收集一次，最多5张)
                    // 注意：必须深拷贝，因为converter返回的对象复用buffer
                    if (processedCount % 10 == 0 && sampleFrames.size() < 5) {
                        sampleFrames.add(copyImage(srcImage));
                    }

                    // 2. 核心算法：缩放 + 抖动 (复用 ImageUtils)
                    BufferedImage dithered = imageUtils.toDitheredImage(srcImage);

                    // 3. 提取纯像素数据 (1-bit 打包)
                    byte[] frameBytes = convertTo1BitRawData(dithered);
                    binOutputStream.write(frameBytes);

                    processedCount++;
                }
            }
            frameIndex++;
        }

        grabber.stop();

        return VideoProcessResult.builder()
                .binStream(new ByteArrayInputStream(binOutputStream.toByteArray()))
                .sampleFrames(sampleFrames)
                .build();
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }

    /**
     * 将抖动后的黑白 BufferedImage 转成 1-bit 的 byte[]
     * 假设屏幕宽是 8 的倍数
     */
    private byte[] convertTo1BitRawData(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        // 每一帧的大小 = 宽 * 高 / 8 (bit转byte)
        byte[] data = new byte[w * h / 8];

        int byteIndex = 0;
        int bitIndex = 0;
        byte currentByte = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // 获取颜色，黑色为0，白色为1 (或者反过来，看你屏幕驱动要求)
                // 这里假设：黑色(0x000000) -> bit 0, 白色(0xFFFFFF) -> bit 1
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                int bit = (rgb == 0) ? 0 : 1;

                // 拼凑 bit 到 byte 中
                // 移位操作：把 bit 塞进去
                currentByte = (byte) ((currentByte << 1) | bit);
                bitIndex++;

                if (bitIndex == 8) {
                    data[byteIndex++] = currentByte;
                    bitIndex = 0;
                    currentByte = 0;
                }
            }
        }
        return data;
    }
}
