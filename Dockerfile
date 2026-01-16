# 基础镜像
FROM registry.cn-hangzhou.aliyuncs.com/xfg-studio/openjdk:17-ea-17-jdk-slim-buster

LABEL maintainer="ywz626"

ARG JAR_FILE=E_Ink_Billboard_Backend-0.0.1-SNAPSHOT.jar
ENV PARAMS=""
ENV TZ=PRC

# 只做最基本的时区设置，不装任何额外软件
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 直接放入 Jar 包
ADD ${JAR_FILE} /app.jar

ENTRYPOINT ["sh", "-c", "java -jar $JAVA_OPTS /app.jar $PARAMS"]