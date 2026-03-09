package com.stalab.e_ink_billboard_backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户活跃度统计响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityVO {
    /**
     * 时间粒度：day|week|month
     */
    private String granularity;

    /**
     * 数据列表
     */
    private List<UserActivityItemVO> items;

    /**
     * 总活跃用户数（统计周期内）
     */
    private Integer totalActiveUsers;

    /**
     * 日均活跃用户数
     */
    private BigDecimal avgDailyActiveUsers;
}
