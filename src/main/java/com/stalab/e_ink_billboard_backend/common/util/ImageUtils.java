package com.stalab.e_ink_billboard_backend.common.util;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class ImageUtils {

    // 墨水屏的标准分辨率 (根据实际的屏幕型号修改，这里假设是 4.2寸 400x300 或 800x480)
    private static final int TARGET_WIDTH = 400;
    private static final int TARGET_HEIGHT = 300;

    /**
     * 核心处理流程：缩放 -> 灰度 -> 抖动 -> 转字节数组
     */
    public ByteArrayInputStream processImage(InputStream input) throws IOException {
        // 1. 强制缩放 (使用 Thumbnailator)
        // 会自动处理 jpg/png 的旋转问题，并强制拉伸到指定分辨率
        BufferedImage resizedImage = Thumbnails.of(input)
                .forceSize(TARGET_WIDTH, TARGET_HEIGHT)
                .outputFormat("png")
                .asBufferedImage();

        // 2. 转为灰度图 (去色)
        BufferedImage grayImage = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grayImage.getGraphics();
        g.drawImage(resizedImage, 0, 0, null);
        g.dispose();

        // 3. 应用 Floyd-Steinberg 抖动算法 (变成纯黑白)
        BufferedImage ditheredImage = applyFloydSteinbergDithering(grayImage);

        // 4. 输出为流 (准备上传到 MinIO)
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(ditheredImage, "png", os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    /**
     * Floyd-Steinberg 抖动算法实现
     */
    private BufferedImage applyFloydSteinbergDithering(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        // 为了防止处理越界，且方便计算，先把像素读到一个二维数组里
        // 这里的 range 是 0-255
        int[][] pixels = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // 获取 RGB 的 Blue 分量作为灰度值 (因为是灰度图，RGB一样)
                pixels[y][x] = img.getRGB(x, y) & 0xFF;
            }
        }

        // 开始遍历 (从左到右，从上到下)
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int oldPixel = pixels[y][x];

                // 1. 阈值量化: 超过128变白(255)，否则变黑(0)
                int newPixel = oldPixel < 128 ? 0 : 255;
                pixels[y][x] = newPixel; // 更新当前像素

                // 2. 计算误差
                int quantError = oldPixel - newPixel;

                // 3. 扩散误差 (四个方向)
                // 右边 (x+1, y) : 7/16
                if (x + 1 < w) {
                    pixels[y][x + 1] = clamp(pixels[y][x + 1] + (int)(quantError * 7.0 / 16));
                }
                // 左下 (x-1, y+1) : 3/16
                if (x - 1 >= 0 && y + 1 < h) {
                    pixels[y + 1][x - 1] = clamp(pixels[y + 1][x - 1] + (int)(quantError * 3.0 / 16));
                }
                // 正下 (x, y+1) : 5/16
                if (y + 1 < h) {
                    pixels[y + 1][x] = clamp(pixels[y + 1][x] + (int)(quantError * 5.0 / 16));
                }
                // 右下 (x+1, y+1) : 1/16
                if (x + 1 < w && y + 1 < h) {
                    pixels[y + 1][x + 1] = clamp(pixels[y + 1][x + 1] + (int)(quantError * 1.0 / 16));
                }
            }
        }

        // 将处理后的数组写回 BufferedImage
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY); // 注意这里是 BINARY
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int val = pixels[y][x];
                // 还原成 RGB 格式 (黑:0x000000, 白:0xFFFFFF)
                int rgb = (val == 0) ? 0x000000 : 0xFFFFFF;
                result.setRGB(x, y, rgb);
            }
        }
        return result;
    }

    /**
     * 辅助方法：确保像素值在 0-255 之间
     */
    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
