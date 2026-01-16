# 基础镜像
FROM registry.cn-hangzhou.aliyuncs.com/xfg-studio/openjdk:17-ea-17-jdk-slim-buster

LABEL maintainer="ywz626"

# 关键修改点：加上 target/ 前缀，因为 Maven 打包后的 jar 在这里面
ARG JAR_FILE=target/E_Ink_Billboard_Backend-0.0.1-SNAPSHOT.jar
ENV PARAMS=""
ENV TZ=PRC

# 设置时区
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 添加应用
ADD ${JAR_FILE} /app.jar

ENTRYPOINT ["sh", "-c", "java -jar $JAVA_OPTS /app.jar $PARAMS"]