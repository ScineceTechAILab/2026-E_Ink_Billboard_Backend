package com.stalab.e_ink_billboard_backend.service.analytic.impl;

import com.stalab.e_ink_billboard_backend.mapper.DeviceMapper;
import com.stalab.e_ink_billboard_backend.mapper.ImageMapper;
import com.stalab.e_ink_billboard_backend.mapper.VideoMapper;
import com.stalab.e_ink_billboard_backend.mapper.po.Device;
import com.stalab.e_ink_billboard_backend.model.vo.DeviceStatusVO;
import com.stalab.e_ink_billboard_backend.model.vo.UserActivityItemVO;
import com.stalab.e_ink_billboard_backend.model.vo.UserActivityVO;
import com.stalab.e_ink_billboard_backend.service.analytic.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 管理员数据分析服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final DeviceMapper deviceMapper;
    private final ImageMapper imageMapper;
    private final VideoMapper videoMapper;

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 获取用户活跃度数据
     */
    @Override
    public UserActivityVO getUserActivity(LocalDate startDate, LocalDate endDate, String granularity) {
        List<UserActivityItemVO> items = new ArrayList<>();

        LocalDate current = startDate;
        Map<String, Integer> previousPeriodData = new HashMap<>();

        // 生成数据点
        while (!current.isAfter(endDate)) {
            String dateKey = formatDate(current, granularity);
            LocalDate periodStart = getPeriodStart(current, granularity);
            LocalDate periodEnd = getPeriodEnd(current, granularity);

            // 计算当前周期活跃用户数
            Set<String> activeUserNames = getActiveUserNames(periodStart, periodEnd);
            int activeUsers = activeUserNames.size();

            // 计算环比（与上一个周期相比）
            BigDecimal monthOnMonth = calculateMonthOnMonth(previousPeriodData, dateKey, activeUsers);

            // 计算同比（与去年同期相比）
            BigDecimal yearOnYear = calculateYearOnYear(periodStart, periodEnd, activeUsers, granularity);

            items.add(UserActivityItemVO.builder()
                    .date(dateKey)
                    .activeUsers(activeUsers)
                    .activeUserNames(new ArrayList<>(activeUserNames))
                    .monthOnMonth(monthOnMonth)
                    .yearOnYear(yearOnYear)
                    .build());

            previousPeriodData.put(dateKey, activeUsers);
            current = getNextPeriod(current, granularity);
        }

        // 计算统计数据（全量去重）
        Set<String> totalActiveUserNames = getActiveUserNames(startDate, endDate);
        int totalActiveUsers = totalActiveUserNames.size();

        BigDecimal avgDailyActiveUsers = items.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(totalActiveUsers).divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);

        return UserActivityVO.builder()
                .granularity(granularity)
                .items(items)
                .totalActiveUsers(totalActiveUsers)
                .avgDailyActiveUsers(avgDailyActiveUsers)
                .build();
    }

    /**
     * 获取设备状态统计
     */
    @Override
    public DeviceStatusVO getDeviceStatus(LocalDate startDate, LocalDate endDate) {
        List<Device> devices = deviceMapper.selectList(null);

        int onlineCount = 0;
        int offlineCount = 0;
        int abnormalCount = 0;

        for (Device device : devices) {
            switch (device.getStatus()) {
                case ONLINE -> onlineCount++;
                case OFFLINE -> offlineCount++;
                default -> abnormalCount++;
            }
        }

        int totalCount = devices.size();
        BigDecimal onlineRate = calculateRate(onlineCount, totalCount);
        BigDecimal offlineRate = calculateRate(offlineCount, totalCount);
        BigDecimal abnormalRate = calculateRate(abnormalCount, totalCount);

        return DeviceStatusVO.builder()
                .onlineCount(onlineCount)
                .offlineCount(offlineCount)
                .abnormalCount(abnormalCount)
                .totalCount(totalCount)
                .onlineRate(onlineRate)
                .offlineRate(offlineRate)
                .abnormalRate(abnormalRate)
                .build();
    }

    /**
     * 获取活跃用户名称集合（查询数据库）
     */
    private Set<String> getActiveUserNames(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // 获取图片上传活跃用户
        List<String> imageUsers = imageMapper.selectActiveUserNicknames(start, end);
        // 获取视频上传活跃用户
        List<String> videoUsers = videoMapper.selectActiveUserNicknames(start, end);

        // 合并并去重
        Set<String> activeUsers = new HashSet<>();
        if (imageUsers != null) {
            activeUsers.addAll(imageUsers);
        }
        if (videoUsers != null) {
            activeUsers.addAll(videoUsers);
        }

        return activeUsers;
    }

    /**
     * 计算环比
     */
    private BigDecimal calculateMonthOnMonth(Map<String, Integer> previousData, String currentKey, int currentValue) {
        if (previousData.isEmpty()) {
            return null;
        }

        // 找到上一个周期的数据
        List<String> keys = new ArrayList<>(previousData.keySet());
        if (keys.isEmpty()) {
            return null;
        }

        String lastKey = keys.get(keys.size() - 1);
        Integer lastValue = previousData.get(lastKey);

        if (lastValue == null || lastValue == 0) {
            return null;
        }

        return BigDecimal.valueOf((currentValue - lastValue) * 100.0 / lastValue)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算同比
     */
    private BigDecimal calculateYearOnYear(LocalDate startDate, LocalDate endDate, int currentValue, String granularity) {
        // 计算去年同期的时间范围
        LocalDate lastYearStart = startDate.minusYears(1);
        LocalDate lastYearEnd = endDate.minusYears(1);

        // 如果是周粒度，需要特殊处理，因为周的起止日期可能会变
        if ("week".equals(granularity)) {
            // 这里简单处理，直接减去52周
            lastYearStart = startDate.minusWeeks(52);
            lastYearEnd = endDate.minusWeeks(52);
        }

        // 获取去年同期的活跃用户数
        Set<String> lastYearUsers = getActiveUserNames(lastYearStart, lastYearEnd);
        int lastYearValue = lastYearUsers.size();

        if (lastYearValue == 0) {
            return null;
        }

        return BigDecimal.valueOf((double) (currentValue - lastYearValue) * 100.0 / lastYearValue)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 格式化日期
     */
    private String formatDate(LocalDate date, String granularity) {
        return switch (granularity) {
            case "week" -> {
                WeekFields weekFields = WeekFields.of(Locale.getDefault());
                int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
                yield date.getYear() + "-W" + String.format("%02d", weekNumber);
            }
            case "month" -> date.format(MONTH_FORMATTER);
            default -> date.format(DAY_FORMATTER);
        };
    }

    /**
     * 获取周期开始日期
     */
    private LocalDate getPeriodStart(LocalDate date, String granularity) {
        return switch (granularity) {
            case "week" -> date.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
            case "month" -> date.withDayOfMonth(1);
            default -> date;
        };
    }

    /**
     * 获取周期结束日期
     */
    private LocalDate getPeriodEnd(LocalDate date, String granularity) {
        return switch (granularity) {
            case "week" -> date.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 7);
            case "month" -> date.withDayOfMonth(date.lengthOfMonth());
            default -> date;
        };
    }

    /**
     * 获取下一个周期
     */
    private LocalDate getNextPeriod(LocalDate date, String granularity) {
        return switch (granularity) {
            case "week" -> date.plusWeeks(1);
            case "month" -> date.plusMonths(1);
            default -> date.plusDays(1);
        };
    }

    /**
     * 计算占比
     */
    private BigDecimal calculateRate(int count, int total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
