package com.stalab.e_ink_billboard_backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户活跃度数据项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityItemVO {
    /**
     * 日期（格式：yyyy-MM-dd 或 yyyy-Www 或 yyyy-MM）
     */
    private String date;

    /**
     * 活跃用户数
     */
    private Integer activeUsers;

    /**
     * 活跃用户名称列表
     */
    private List<String> activeUserNames;

    /**
     * 同比变化百分比（与上一个同期相比）
     */
    private BigDecimal yearOnYear;

    /**
     * 环比变化百分比（与上一个周期相比）
     */
    private BigDecimal monthOnMonth;
}
