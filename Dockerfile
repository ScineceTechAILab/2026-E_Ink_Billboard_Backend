FROM openjdk:17-jdk-slim

LABEL maintainer="ywz626"

# ==========================================
# 2. 环境设置：时区与变量
# ==========================================
ENV TZ=PRC
ENV PARAMS=""

# 设置时区
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone


RUN (sed -i 's/deb.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list.d/debian.sources 2>/dev/null || \
     sed -i 's/deb.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list) && \
    (sed -i 's/security.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list.d/debian.sources 2>/dev/null || \
     sed -i 's/security.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list) && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
    ffmpeg \
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


ARG JAR_FILE=target/*.jar

# 将 Jar 包复制到容器根目录
COPY ${JAR_FILE} /app.jar


EXPOSE 8083

# 启动命令
# -Djava.security.egd=file:/dev/./urandom用于加速 Tomcat 启动
ENTRYPOINT ["sh", "-c", "java -Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom -jar /app.jar $PARAMS"]