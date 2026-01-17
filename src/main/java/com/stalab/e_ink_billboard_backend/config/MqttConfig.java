package com.stalab.e_ink_billboard_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * MQTT配置类
 * 配置MQTT客户端连接、发布和订阅功能
 */
@Slf4j
@Configuration
@IntegrationComponentScan
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.qos:1}")
    private int qos;

    /**
     * 创建MQTT客户端工厂
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();

        // 设置broker地址
        options.setServerURIs(new String[]{brokerUrl});

        // 设置用户名和密码
        options.setUserName(username);
        options.setPassword(password.toCharArray());

        // 设置自动重连
        options.setAutomaticReconnect(true);

        // 设置清理会话（false表示持久会话，离线消息可以保留）
        options.setCleanSession(false);

        // 设置连接超时时间（秒）
        options.setConnectionTimeout(30);

        // 设置保活间隔（秒）
        options.setKeepAliveInterval(60);

        factory.setConnectionOptions(options);

        log.info("MQTT客户端工厂配置完成，broker: {}", brokerUrl);
        return factory;
    }

    /**
     * 发布消息通道
     */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT消息发布处理器
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutboundHandler() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(clientId, mqttClientFactory());
        messageHandler.setAsync(true); // 异步发送
        // 不设置defaultTopic，通过消息头中的mqtt_topic动态指定
        messageHandler.setDefaultQos(qos);

        // 设置消息转换器（明确指定UTF-8编码，避免中文乱码）
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter(qos, false, "UTF-8");
        messageHandler.setConverter(converter);

        log.info("MQTT消息发布处理器配置完成");
        return messageHandler;
    }

    /**
     * 订阅消息通道
     */
    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT消息订阅适配器
     * 订阅设备状态上报和心跳消息
     */
    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter() {
        // 订阅主题：device/+/status 和 device/+/heartbeat
        String[] topics = {
            "device/+/status",      // 设备状态上报
            "device/+/heartbeat"     // 设备心跳
        };

        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
            clientId + "_subscriber",
            mqttClientFactory(),
            topics
        );

        // 增加订阅超时时间（毫秒），避免订阅超时
        adapter.setCompletionTimeout(30000); // 30秒
        adapter.setQos(qos);
        adapter.setOutputChannel(mqttInboundChannel());

        // 设置自动启动（默认true）
        adapter.setAutoStartup(true);

        // 设置消息转换器（保持byte[]格式，在监听器中手动用UTF-8解码，避免中文乱码）
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(true); // 保持原始byte[]，不自动转换为String
        adapter.setConverter(converter);

        log.info("MQTT消息订阅适配器配置完成，订阅主题: {}", String.join(", ", topics));
        log.info("订阅客户端ID: {}", clientId + "_subscriber");
        return adapter;
    }
}
