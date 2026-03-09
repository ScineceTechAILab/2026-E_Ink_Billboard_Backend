package com.stalab.e_ink_billboard_backend.controller;

import com.stalab.e_ink_billboard_backend.common.Response;
import com.stalab.e_ink_billboard_backend.common.enums.UserRole;
import com.stalab.e_ink_billboard_backend.common.util.JwtUtils;
import com.stalab.e_ink_billboard_backend.model.vo.DeviceStatusVO;
import com.stalab.e_ink_billboard_backend.model.vo.UserActivityVO;
import com.stalab.e_ink_billboard_backend.service.analytic.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 管理员数据分析接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;
    private final JwtUtils jwtUtils;

    /**
     * 获取用户活跃度数据
     * GET /api/admin/analytics/user-activity
     */
    @GetMapping("/user-activity")
    public Response<UserActivityVO> getUserActivity(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestHeader("Authorization") String token) {

        if (!jwtUtils.validateToken(token)) {
            return Response.<UserActivityVO>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        String userRole = jwtUtils.getRole(token);
        if (!UserRole.ADMIN.getCode().equals(userRole)) {
            return Response.<UserActivityVO>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        try {
            // 验证时间范围
            if (startDate.isAfter(endDate)) {
                return Response.<UserActivityVO>builder()
                        .code(400)
                        .info("开始日期不能晚于结束日期")
                        .build();
            }

            // 验证时间跨度不超过 90 天
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            if (daysBetween > 90) {
                return Response.<UserActivityVO>builder()
                        .code(400)
                        .info("查询时间跨度不能超过 90 天")
                        .build();
            }

            // 验证 granularity
            if (!"day".equals(granularity) && !"week".equals(granularity) && !"month".equals(granularity)) {
                return Response.<UserActivityVO>builder()
                        .code(400)
                        .info("时间粒度必须为 day、week 或 month")
                        .build();
            }

            UserActivityVO data = adminAnalyticsService.getUserActivity(startDate, endDate, granularity);
            return Response.<UserActivityVO>builder()
                    .code(200)
                    .info("查询成功")
                    .data(data)
                    .build();
        } catch (Exception e) {
            log.error("Get user activity error", e);
            return Response.<UserActivityVO>builder()
                    .code(500)
                    .info("服务器错误")
                    .build();
        }
    }

    /**
     * 获取设备状态统计
     * GET /api/admin/analytics/device-status
     */
    @GetMapping("/device-status")
    public Response<DeviceStatusVO> getDeviceStatus(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestHeader("Authorization") String token) {

        if (!jwtUtils.validateToken(token)) {
            return Response.<DeviceStatusVO>builder()
                    .code(401)
                    .info("Token 无效")
                    .build();
        }

        String userRole = jwtUtils.getRole(token);
        if (!UserRole.ADMIN.getCode().equals(userRole)) {
            return Response.<DeviceStatusVO>builder()
                    .code(403)
                    .info("无权执行此操作，需要管理员权限")
                    .build();
        }

        try {
            // 如果未传日期，默认使用最近 7 天
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(7);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            DeviceStatusVO data = adminAnalyticsService.getDeviceStatus(startDate, endDate);
            return Response.<DeviceStatusVO>builder()
                    .code(200)
                    .info("查询成功")
                    .data(data)
                    .build();
        } catch (Exception e) {
            log.error("Get device status error", e);
            return Response.<DeviceStatusVO>builder()
                    .code(500)
                    .info("服务器错误")
                    .build();
        }
    }
}
