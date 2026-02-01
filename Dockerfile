# 基础镜像
FROM registry.cn-hangzhou.aliyuncs.com/xfg-studio/openjdk:17-ea-17-jdk-slim-buster

LABEL maintainer="ywz626"

# 安装 JavaCV/FFmpeg 依赖库 (解决 NoClassDefFoundError: FFmpegFrameGrabber)
# 替换源为阿里云源以加速构建
RUN sed -i 's/deb.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    sed -i 's/security.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
    libgtk2.0-0 \
    libgl1-mesa-glx \
    libx11-xcb1 \
    libxcb1 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libfontconfig1 \
    && rm -rf /var/lib/apt/lists/*

# 关键修改点：加上 target/ 前缀，因为 Maven 打包后的 jar 在这里面
ARG JAR_FILE=target/E_Ink_Billboard_Backend-0.0.1-SNAPSHOT.jar
ENV PARAMS=""
ENV TZ=PRC

# 设置时区
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 添加应用
ADD ${JAR_FILE} /app.jar

# 添加 -Djava.awt.headless=true 参数
ENTRYPOINT ["sh", "-c", "java -Djava.awt.headless=true -jar $JAVA_OPTS /app.jar $PARAMS"]
