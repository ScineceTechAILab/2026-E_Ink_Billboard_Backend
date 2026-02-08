package com.stalab.e_ink_billboard_backend.service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stalab.e_ink_billboard_backend.model.dto.MqttHeartbeatMessage;
import com.stalab.e_ink_billboard_backend.model.dto.MqttStatusMessage;
import com.stalab.e_ink_billboard_backend.service.push.PushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * MQTT消息监听器
 * 监听设备状态上报和心跳消息
 */
@Slf4j
@Component
public class MqttMessageListener implements MessageHandler {

    @Autowired
    private PushService pushService;

    private final ObjectMapper objectMapper;

    public MqttMessageListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("MQTT消息监听器已初始化");
    }

    /**
     * 处理接收到的MQTT消息
     * 通过@ServiceActivator注解绑定到mqttInboundChannel
     */
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        try {
            // 打印所有消息头，用于调试
            log.info("收到MQTT消息，消息头: {}", message.getHeaders());

            // 获取消息主题（尝试多个可能的键名）
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            if (topic == null) {
                topic = (String) message.getHeaders().get("mqtt_topic");
            }
            if (topic == null) {
                topic = (String) message.getHeaders().get("topic");
            }
            if (topic == null) {
                log.warn("收到MQTT消息但缺少topic信息，所有消息头: {}", message.getHeaders().keySet());
                return;
            }

            // 获取消息内容
            Object payload = message.getPayload();
            log.debug("MQTT消息payload类型: {}, 类名: {}", payload.getClass().getName(), payload.getClass());

            String payloadStr;
            if (payload instanceof String) {
                // 如果已经是String，可能是转换器已经转换了，但可能使用了错误编码
                // 这种情况下，我们需要尝试从原始byte[]重新解码
                payloadStr = (String) payload;
                log.warn("收到String类型的payload，可能存在编码问题。如果出现乱码，请检查消息转换器配置。");
            } else if (payload instanceof byte[]) {
                // 使用UTF-8编码解析字节数组，避免中文乱码
                byte[] payloadBytes = (byte[]) payload;
                payloadStr = new String(payloadBytes, StandardCharsets.UTF_8);
                log.debug("从byte[]解码payload，长度: {} 字节", payloadBytes.length);
            } else {
                payloadStr = payload.toString();
                log.warn("收到未知类型的payload: {}", payload.getClass().getName());
            }

            log.info("收到MQTT消息: topic={}, payload={}", topic, payloadStr);

            // 从topic中提取deviceCode
            // topic格式：device/{deviceCode}/status 或 device/{deviceCode}/heartbeat
            String deviceCode = extractDeviceCode(topic);
            if (deviceCode == null) {
                log.warn("无法从topic中提取deviceCode: topic={}", topic);
                return;
            }

            // 根据topic类型处理不同的消息
            if (topic.endsWith("/status")) {
                // 状态上报消息
                handleStatusMessage(deviceCode, payloadStr);
            } else if (topic.endsWith("/heartbeat")) {
                // 心跳消息
                handleHeartbeatMessage(deviceCode, payloadStr);
            } else {
                log.warn("未知的MQTT消息类型: topic={}", topic);
            }

        } catch (Exception e) {
            log.error("处理MQTT消息失败", e);
        }
    }

    /**
     * 从topic中提取deviceCode
     * topic格式：device/{deviceCode}/status 或 device/{deviceCode}/heartbeat
     *
     * @param topic MQTT主题
     * @return 设备编码，如果提取失败返回null
     */
    private String extractDeviceCode(String topic) {
        try {
            // topic格式：device/{deviceCode}/status 或 device/{deviceCode}/heartbeat
            String[] parts = topic.split("/");
            if (parts.length >= 2 && "device".equals(parts[0])) {
                return parts[1];
            }
        } catch (Exception e) {
            log.error("提取deviceCode失败: topic={}", topic, e);
        }
        return null;
    }

    /**
     * 处理状态上报消息
     *
     * @param deviceCode 设备编码
     * @param payloadStr 消息内容（JSON字符串）
     */
    private void handleStatusMessage(String deviceCode, String payloadStr) {
        try {
            if (payloadStr == null || payloadStr.trim().isEmpty()) {
                log.warn("收到空的状态上报消息，忽略: deviceCode={}", deviceCode);
                return;
            }

            // 解析JSON消息
            MqttStatusMessage statusMessage = objectMapper.readValue(payloadStr, MqttStatusMessage.class);

            // 调用PushService处理
            pushService.handleStatusMessage(deviceCode, statusMessage);
        } catch (Exception e) {
            log.error("处理状态上报消息失败: deviceCode={}, payload={}", deviceCode, payloadStr, e);
        }
    }

    /**
     * 处理心跳消息
     *
     * @param deviceCode 设备编码
     * @param payloadStr 消息内容（JSON字符串）
     */
    private void handleHeartbeatMessage(String deviceCode, String payloadStr) {
        try {
            log.info("开始处理心跳消息: deviceCode={}, payload={}", deviceCode, payloadStr);

            MqttHeartbeatMessage heartbeatMessage;
            if (payloadStr == null || payloadStr.trim().isEmpty()) {
                log.info("收到空的心跳消息，作为Ping处理: deviceCode={}", deviceCode);
                heartbeatMessage = new MqttHeartbeatMessage();
            } else {
                // 解析JSON消息
                heartbeatMessage = objectMapper.readValue(payloadStr, MqttHeartbeatMessage.class);
            }

            log.info("心跳消息解析成功: deviceCode={}, status={}", deviceCode, heartbeatMessage.getStatus());

            // 调用PushService处理
            pushService.handleHeartbeat(deviceCode, heartbeatMessage);
            log.info("心跳消息处理完成: deviceCode={}", deviceCode);
        } catch (Exception e) {
            log.error("处理心跳消息失败: deviceCode={}, payload={}", deviceCode, payloadStr, e);
        }
    }
}
