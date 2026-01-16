import numpy as np
import cv2
import time
import os

# 播放.bin视频脚本

# ================= 配置区域 =================
# 1. 你的 .bin 文件路径
BIN_FILE_PATH = 'video.bin'

# 2. 屏幕分辨率
WIDTH = 400
HEIGHT = 300

# 3. 播放速度 (帧间隔，单位秒)
FRAME_INTERVAL = 0.2  # 5fps -> 0.2s
# ===========================================

def play_bin_video():
    if not os.path.exists(BIN_FILE_PATH):
        print(f"错误: 找不到文件 {BIN_FILE_PATH}")
        return

    # 1. 计算每一帧的字节数
    # 逻辑: (宽 x 高) / 8 bit
    frame_size = (WIDTH * HEIGHT) // 8
    print(f"分辨率: {WIDTH}x{HEIGHT}, 每帧大小: {frame_size} bytes")

    # 2. 读取整个文件
    try:
        # 以 uint8 (字节) 格式读取所有数据
        raw_data = np.fromfile(BIN_FILE_PATH, dtype=np.uint8)
        print(f"文件总大小: {len(raw_data)} bytes")

        # 计算总帧数
        total_frames = len(raw_data) // frame_size
        print(f"估算总帧数: {total_frames}")

        if total_frames == 0:
            print("错误: 文件太小，不足一帧")
            return

    except Exception as e:
        print(f"读取文件失败: {e}")
        return

    # 3. 循环播放
    current_frame = 0

    print("开始播放... 按 'q' 退出")

    while current_frame < total_frames:
        # A. 截取当前帧的字节数据
        start_idx = current_frame * frame_size
        end_idx = start_idx + frame_size
        frame_bytes = raw_data[start_idx:end_idx]

        if len(frame_bytes) < frame_size:
            break

        # B. 核心解包: 把 1个byte 拆成 8个bit (0或1)
        # unpackbits 默认是从高位到低位解析 (big-endian)，这符合通常的移位逻辑
        bits = np.unpackbits(frame_bytes)

        # C. 此时 bits 是一个一维数组，包含了 0 和 1
        # 我们把它变回 (Height, Width) 的二维矩阵
        # 注意: 这里的 reshape 顺序要看你 Java 是怎么写的，通常是先 Y 后 X
        try:
            image_buffer = bits.reshape(HEIGHT, WIDTH)
        except ValueError:
            print(f"错误: 数据无法 reshape 成 {WIDTH}x{HEIGHT}，请检查分辨率配置！")
            break

        # D. 转换成可视化的图片 (0->0黑色, 1->255白色)
        #astype(np.uint8) 是必须的，cv2 需要 uint8 类型
        display_img = (image_buffer * 255).astype(np.uint8)

        # E. 显示
        cv2.imshow('E-Ink Preview Player', display_img)

        # 打印进度
        print(f"Frame: {current_frame + 1}/{total_frames}", end='\r')

        # F. 控制帧率
        key = cv2.waitKey(int(FRAME_INTERVAL * 1000)) & 0xFF
        if key == ord('q'): # 按 q 退出
            break

        current_frame += 1

        # 循环播放逻辑 (如果想要循环，把下面注释打开)
        # if current_frame >= total_frames:
        #     current_frame = 0

    cv2.destroyAllWindows()
    print("\n播放结束")

if __name__ == '__main__':
    play_bin_video()