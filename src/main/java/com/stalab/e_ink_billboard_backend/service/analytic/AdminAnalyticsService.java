package com.stalab.e_ink_billboard_backend.service.analytic;

import com.stalab.e_ink_billboard_backend.model.vo.DeviceStatusVO;
import com.stalab.e_ink_billboard_backend.model.vo.UserActivityVO;

import java.time.LocalDate;

/**
 * 管理员数据分析服务接口
 */
public interface AdminAnalyticsService {

    /**
     * 获取用户活跃度数据
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param granularity 时间粒度：day|week|month
     * @return 用户活跃度数据
     */
    UserActivityVO getUserActivity(LocalDate startDate, LocalDate endDate, String granularity);

    /**
     * 获取设备状态统计
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 设备状态统计
     */
    DeviceStatusVO getDeviceStatus(LocalDate startDate, LocalDate endDate);
}
