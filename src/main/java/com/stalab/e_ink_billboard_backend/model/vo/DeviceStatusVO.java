package com.stalab.e_ink_billboard_backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 设备状态统计响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatusVO {
    /**
     * 在线设备数量
     */
    private Integer onlineCount;

    /**
     * 离线设备数量
     */
    private Integer offlineCount;

    /**
     * 异常设备数量
     */
    private Integer abnormalCount;

    /**
     * 设备总数
     */
    private Integer totalCount;

    /**
     * 在线率
     */
    private BigDecimal onlineRate;

    /**
     * 离线率
     */
    private BigDecimal offlineRate;

    /**
     * 异常率
     */
    private BigDecimal abnormalRate;
}
