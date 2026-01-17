package com.stalab.e_ink_billboard_backend.service;

import cn.hutool.json.JSONUtil;
import com.stalab.e_ink_billboard_backend.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

/**
 * MQTT服务类
 * 提供MQTT消息发布功能
 * 注意：订阅功能在MqttConfig中通过MqttPahoMessageDrivenChannelAdapter配置
 * 消息接收通过@ServiceActivator处理（见MqttMessageListener）
 */
@Slf4j
@Service
public class MqttService {

    @Autowired
    private MessageChannel mqttOutboundChannel;

    @Value("${mqtt.qos:1}")
    private int defaultQos;

    /**
     * 发布消息到指定topic
     *
     * @param topic MQTT主题
     * @param payload 消息内容（对象，会自动序列化为JSON）
     */
    public void publish(String topic, Object payload) {
        publish(topic, payload, defaultQos);
    }

    /**
     * 发布消息到指定topic（指定QoS）
     *
     * @param topic MQTT主题
     * @param payload 消息内容（对象，会自动序列化为JSON）
     * @param qos 服务质量等级（0, 1, 2）
     */
    public void publish(String topic, Object payload, int qos) {
        try {
            // 将对象序列化为JSON字符串
            String jsonPayload;
            if (payload instanceof String) {
                jsonPayload = (String) payload;
            } else {
                jsonPayload = JSONUtil.toJsonStr(payload);
            }

            // 构建MQTT消息
            Message<String> message = MessageBuilder.withPayload(jsonPayload)
                    .setHeader("mqtt_topic", topic)
                    .setHeader("mqtt_qos", qos)
                    .build();

            // 发送消息
            boolean sent = mqttOutboundChannel.send(message);

            if (sent) {
                log.info("MQTT消息发布成功，topic: {}, payload: {}", topic, jsonPayload);
            } else {
                log.error("MQTT消息发布失败，topic: {}, payload: {}", topic, jsonPayload);
                throw new BusinessException(500, "MQTT消息发布失败");
            }
        } catch (Exception e) {
            log.error("MQTT消息发布异常，topic: {}, payload: {}", topic, payload, e);
            throw new BusinessException(500, "MQTT消息发布异常: " + e.getMessage());
        }
    }

    /**
     * 获取订阅的消息通道（用于手动处理消息）
     * 注意：订阅功能在MqttConfig中配置，消息接收通过@ServiceActivator处理
     *
     * @return 消息通道
     */
    public MessageChannel getInboundChannel() {
        // 注意：订阅功能已在MqttConfig中通过MqttPahoMessageDrivenChannelAdapter配置
        // 消息接收通过MqttMessageListener中的@ServiceActivator处理
        return null; // 此方法保留用于未来扩展
    }
}
