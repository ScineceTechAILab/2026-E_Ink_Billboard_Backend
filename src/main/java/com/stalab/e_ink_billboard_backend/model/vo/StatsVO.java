package com.stalab.e_ink_billboard_backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: ywz
 * @CreateTime: 2026-01-07
 * @Description: 统计数据VO
 * @Version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsVO {
    /**
     * 在线设备数量
     */
    private Integer onlineDevices;

    /**
     * 待审核内容数量
     */
    private Integer pendingAudits;

    /**
     * 已通过内容数量
     */
    private Integer approvedContent;

    // 兼容字段名（可选）
    private Integer online;
    private Integer pending;
    private Integer approved;
}
