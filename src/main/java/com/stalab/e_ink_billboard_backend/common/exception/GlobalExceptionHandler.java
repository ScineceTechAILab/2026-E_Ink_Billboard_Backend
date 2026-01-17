package com.stalab.e_ink_billboard_backend.common.exception;

import com.stalab.e_ink_billboard_backend.common.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 2. 拦截自定义业务异常 (BusinessException)
     * 场景：你代码里手动 throw new BusinessException("余额不足");
     */
    @ExceptionHandler(BusinessException.class)
    public Response<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Response.builder()
                .code(e.getCode())
                .info(e.getMessage())
                .build();
    }

    /**
     * 3. 拦截参数校验异常 (@Valid / @Validated)
     * 场景：DTO 里写了 @NotNull，但前端没传值，Spring 会抛出这个异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<?> handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        // 获取第一条错误提示，比如 "密码不能为空"
        String message = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
        log.warn("参数校验失败: {}", message);
        return Response.builder()
                .code(400)
                .info(message)
                .build();
    }

    /**
     * 4. 拦截数据库唯一索引冲突 (DuplicateKeyException)
     * 场景：OpenID 设置了 unique，如果插入重复用户，数据库会报错
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Response<?> handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("数据库唯一约束冲突", e);
        return Response.builder().code(400).info("数据已存在，请勿重复操作").build();
    }

    /**
     * 5. 拦截所有未知的系统异常 (兜底方案)
     * 场景：空指针 (NPE)、数组越界、SQL写错了等
     */
    @ExceptionHandler(Exception.class)
    public Response<?> handleException(Exception e) {
        log.error("系统未知错误", e); // 打印堆栈到控制台，方便你排查
        // 千万别把 e.getMessage() 直接给前端，可能会暴露 SQL 结构等敏感信息
        return Response.builder().code(500).info("系统繁忙，请稍后再试").build();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Response<?> handleMaxSizeException(MaxUploadSizeExceededException e) {
        log.warn("文件上传超限: {}", e.getMessage());
        return Response.builder()
                .code(400)
                .info("上传文件大小超出限制，请上传 50MB 以内的文件")
                .build();
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Response<?> handleNoResourceFoundException(NoResourceFoundException e) {
        // 这种错误不需要打印堆栈，只需要告诉前端 404 即可
        return Response.builder().code(404).info("路径不存在，请检查 URL 是否正确").build();
        // 或者返回 null，由 Spring Boot 默认处理
    }

    /**
     * 处理MQTT相关异常
     * 场景：MQTT连接失败、消息发送失败等
     */
    @ExceptionHandler(org.eclipse.paho.client.mqttv3.MqttException.class)
    public Response<?> handleMqttException(org.eclipse.paho.client.mqttv3.MqttException e) {
        log.error("MQTT异常: {}", e.getMessage(), e);
        // 根据错误码返回不同的提示信息
        String message;
        switch (e.getReasonCode()) {
            case org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_BROKER_UNAVAILABLE:
                message = "MQTT服务器不可用，请稍后重试";
                break;
            case org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_NOT_CONNECTED:
                message = "MQTT客户端未连接，请稍后重试";
                break;
            case org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_CLIENT_TIMEOUT:
                message = "MQTT连接超时，请稍后重试";
                break;
            default:
                message = "MQTT通信失败，请稍后重试";
        }
        return Response.builder()
                .code(500)
                .info(message)
                .build();
    }

    /**
     * 处理消息通道异常（Spring Integration MQTT）
     * 场景：MQTT消息发送到通道失败
     */
    @ExceptionHandler(org.springframework.messaging.MessageDeliveryException.class)
    public Response<?> handleMessageDeliveryException(org.springframework.messaging.MessageDeliveryException e) {
        log.error("MQTT消息发送失败", e);
        return Response.builder()
                .code(500)
                .info("消息发送失败，请稍后重试")
                .build();
    }
}
