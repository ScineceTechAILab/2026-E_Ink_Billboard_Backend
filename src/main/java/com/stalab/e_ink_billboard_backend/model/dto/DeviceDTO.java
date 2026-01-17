package com.stalab.e_ink_billboard_backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 设备DTO（用于添加和更新设备）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDTO {
    /**
     * 设备ID（更新时必填）
     */
    private Long id;

    /**
     * 设备名称
     */
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100")
    private String deviceName;

    /**
     * 设备编码（唯一标识）
     */
    @NotBlank(message = "设备编码不能为空")
    @Size(max = 50, message = "设备编码长度不能超过50")
    private String deviceCode;

    /**
     * MQTT主题（可选，不提供则自动生成）
     */
    @Size(max = 200, message = "MQTT主题长度不能超过200")
    private String mqttTopic;

    /**
     * 位置信息（可选）
     */
    @Size(max = 200, message = "位置信息长度不能超过200")
    private String location;

    /**
     * 设备描述（可选）
     */
    private String description;
}
