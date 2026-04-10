<template>
  <div class="ink-analytics-page">
    <!-- Background -->
    <div class="ink-grid-bg" />

    <!-- Header -->
    <header class="ink-page-header">
      <div class="ink-container">
        <div class="ink-header-content">
          <div class="ink-header-brand">
            <div class="ink-header-icon ink-glow-box">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="20" x2="18" y2="10" />
                <line x1="12" y1="20" x2="12" y2="4" />
                <line x1="6" y1="20" x2="6" y2="14" />
              </svg>
            </div>
            <div>
              <h1 class="ink-heading ink-heading-4">数据统计</h1>
              <p class="ink-body">洞察业务数据，做出明智决策</p>
            </div>
          </div>
          <button @click="router.push('/dashboard')" class="ink-btn ink-btn-secondary">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            返回仪表台
          </button>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="ink-page-main">
      <div class="ink-container">
        <!-- Time Filter -->
        <div class="ink-card ink-filter-bar">
          <div class="ink-filter-row">
            <div class="ink-filter-group">
              <span class="ink-filter-label">时间范围：</span>
              <div class="ink-time-toggle">
                <button
                  v-for="range in timeRanges"
                  :key="range.value"
                  :class="['ink-toggle-btn', { 'ink-toggle-active': timeRange === range.value }]"
                  @click="handleTimeRangeChange(range.value)"
                >
                  {{ range.label }}
                </button>
              </div>
              <el-date-picker
                v-if="timeRange === 'custom'"
                v-model="customDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                :shortcuts="dateShortcuts"
                :disabled-date="disabledDate"
                @change="loadAllData"
                class="ink-date-picker"
              />
            </div>
            <div class="ink-filter-group">
              <span class="ink-filter-label">粒度：</span>
              <div class="ink-granularity-toggle">
                <button
                  v-for="g in granularities"
                  :key="g.value"
                  :class="['ink-toggle-btn ink-toggle-sm', { 'ink-toggle-active': granularity === g.value }]"
                  @click="handleGranularityChange(g.value)"
                >
                  {{ g.label }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Stats Overview -->
        <div class="ink-grid ink-grid-4">
          <div class="ink-stat-card ink-animate-slide-up">
            <div class="ink-stat-header">
              <div class="ink-stat-icon" style="background: rgba(6, 182, 212, 0.1); color: #06b6d4;">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" />
                </svg>
              </div>
            </div>
            <div class="ink-stat-value">{{ userActivityData?.totalActiveUsers || 0 }}</div>
            <div class="ink-stat-label">活跃用户</div>
          </div>

          <div class="ink-stat-card ink-animate-slide-up ink-stagger-1">
            <div class="ink-stat-header">
              <div class="ink-stat-icon" style="background: rgba(16, 185, 129, 0.1); color: #10b981;">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
                  <polyline points="17 6 23 6 23 12" />
                </svg>
              </div>
            </div>
            <div class="ink-stat-value">{{ userActivityData?.avgDailyActiveUsers || 0 }}</div>
            <div class="ink-stat-label">日均活跃</div>
          </div>

          <div class="ink-stat-card ink-animate-slide-up ink-stagger-2">
            <div class="ink-stat-header">
              <div class="ink-stat-icon" style="background: rgba(139, 92, 246, 0.1); color: #8b5cf6;">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                  <line x1="8" y1="21" x2="16" y2="21" />
                  <line x1="12" y1="17" x2="12" y2="21" />
                </svg>
              </div>
            </div>
            <div class="ink-stat-value">{{ deviceStatusData?.onlineCount || 0 }}</div>
            <div class="ink-stat-label">在线设备</div>
          </div>

          <div class="ink-stat-card ink-animate-slide-up ink-stagger-3">
            <div class="ink-stat-header">
              <div class="ink-stat-icon" style="background: rgba(245, 158, 11, 0.1); color: #f59e0b;">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
                </svg>
              </div>
            </div>
            <div class="ink-stat-value">{{ deviceStatusData?.onlineRate || 0 }}%</div>
            <div class="ink-stat-label">在线率</div>
          </div>
        </div>

        <!-- Charts Grid -->
        <div class="ink-grid ink-grid-2 ink-mt-6">
          <!-- User Activity Chart -->
          <div class="ink-card ink-chart-card ink-chart-lg">
            <div class="ink-chart-header">
              <h3 class="ink-chart-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                </svg>
                用户活跃度趋势
              </h3>
              <div class="ink-compare-toggle">
                <button
                  :class="['ink-toggle-btn ink-toggle-sm', { 'ink-toggle-active': compareType === 'mom' }]"
                  @click="compareType = 'mom'"
                >
                  环比
                </button>
                <button
                  :class="['ink-toggle-btn ink-toggle-sm', { 'ink-toggle-active': compareType === 'yoy' }]"
                  @click="compareType = 'yoy'"
                >
                  同比
                </button>
              </div>
            </div>
            <div v-loading="loading.activity" class="ink-chart-container">
              <v-chart
                v-if="userActivityOption"
                :option="userActivityOption"
                class="ink-chart"
                autoresize
              />
              <div v-else class="ink-chart-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                </svg>
                <p>暂无数据</p>
              </div>
            </div>
          </div>

          <!-- Device Status Pie -->
          <div class="ink-card ink-chart-card">
            <h3 class="ink-chart-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <path d="M12 6v6l4 2" />
              </svg>
              设备状态分布
            </h3>
            <div v-loading="loading.device" class="ink-chart-container">
              <v-chart
                v-if="deviceStatusOption"
                :option="deviceStatusOption"
                class="ink-chart"
                autoresize
                @click="handleDevicePieClick"
              />
              <div v-else class="ink-chart-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <circle cx="12" cy="12" r="10" />
                  <path d="M12 6v6l4 2" />
                </svg>
                <p>暂无数据</p>
              </div>
            </div>
          </div>

          <!-- Device Status Details -->
          <div class="ink-card ink-status-card">
            <h3 class="ink-chart-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="8" y1="6" x2="21" y2="6" />
                <line x1="8" y1="12" x2="21" y2="12" />
                <line x1="8" y1="18" x2="21" y2="18" />
                <line x1="3" y1="6" x2="3.01" y2="6" />
                <line x1="3" y1="12" x2="3.01" y2="12" />
                <line x1="3" y1="18" x2="3.01" y2="18" />
              </svg>
              设备状态详情
            </h3>
            <div class="ink-status-list">
              <div class="ink-status-item ink-status-online">
                <div class="ink-status-indicator">
                  <span class="ink-status-dot" />
                  <span class="ink-status-name">在线</span>
                </div>
                <div class="ink-status-numbers">
                  <span class="ink-status-count">{{ deviceStatusData?.onlineCount || 0 }}</span>
                  <span class="ink-status-rate">{{ deviceStatusData?.onlineRate || 0 }}%</span>
                </div>
              </div>
              <div class="ink-status-item ink-status-offline">
                <div class="ink-status-indicator">
                  <span class="ink-status-dot" />
                  <span class="ink-status-name">离线</span>
                </div>
                <div class="ink-status-numbers">
                  <span class="ink-status-count">{{ deviceStatusData?.offlineCount || 0 }}</span>
                  <span class="ink-status-rate">{{ deviceStatusData?.offlineRate || 0 }}%</span>
                </div>
              </div>
              <div class="ink-status-item ink-status-error">
                <div class="ink-status-indicator">
                  <span class="ink-status-dot" />
                  <span class="ink-status-name">异常</span>
                </div>
                <div class="ink-status-numbers">
                  <span class="ink-status-count">{{ deviceStatusData?.abnormalCount || 0 }}</span>
                  <span class="ink-status-rate">{{ deviceStatusData?.abnormalRate || 0 }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent
} from 'echarts/components'
import type { EChartsOption } from 'echarts'
import { ElMessage } from 'element-plus'
import { analyticsApi } from '@/api'
import type { UserActivityVO, DeviceStatusVO } from '@/types'

use([
  CanvasRenderer,
  LineChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent
])

const router = useRouter()

// Time ranges
const timeRanges = [
  { label: '最近7天', value: '7d' },
  { label: '最近30天', value: '30d' },
  { label: '最近90天', value: '90d' },
  { label: '自定义', value: 'custom' }
]

const granularities = [
  { label: '日', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' }
]

const timeRange = ref<'7d' | '30d' | '90d' | 'custom'>('7d')
const granularity = ref<'day' | 'week' | 'month'>('day')
const compareType = ref<'mom' | 'yoy'>('mom')
const customDateRange = ref<[Date, Date] | null>(null)

const loading = ref({
  activity: false,
  device: false
})

const userActivityData = ref<UserActivityVO | null>(null)
const deviceStatusData = ref<DeviceStatusVO | null>(null)

const dateShortcuts = [
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近一个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  },
  {
    text: '最近三个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
      return [start, end]
    }
  }
]

const disabledDate = (time: Date) => {
  const ninetyDaysAgo = new Date()
  ninetyDaysAgo.setTime(ninetyDaysAgo.getTime() - 3600 * 1000 * 24 * 90)
  return time.getTime() > Date.now() || time.getTime() < ninetyDaysAgo.getTime()
}

const dateRange = computed(() => {
  if (timeRange.value === 'custom' && customDateRange.value) {
    return {
      start: formatDate(customDateRange.value[0]),
      end: formatDate(customDateRange.value[1])
    }
  }

  const end = new Date()
  let start = new Date()

  switch (timeRange.value) {
    case '7d':
      start.setDate(end.getDate() - 7)
      break
    case '30d':
      start.setDate(end.getDate() - 30)
      break
    case '90d':
      start.setDate(end.getDate() - 90)
      break
  }

  return {
    start: formatDate(start),
    end: formatDate(end)
  }
})

const userActivityOption = computed<EChartsOption>(() => {
  if (!userActivityData.value?.items.length) return null

  const items = userActivityData.value.items

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(20, 20, 22, 0.9)',
      borderColor: 'rgba(255, 255, 255, 0.1)',
      textStyle: { color: '#fafaf9' },
      formatter: (params: any) => {
        const item = items[params[0].dataIndex]
        let result = `<div style="font-weight: 600; margin-bottom: 8px;">${params[0].axisValue}</div>`
        result += `<div style="display: flex; align-items: center; gap: 8px;">
          <span style="display: inline-block; width: 8px; height: 8px; background: #10b981; border-radius: 50%;"></span>
          <span>活跃用户: ${item.activeUsers}</span>
        </div>`

        if (item.activeUserNames && item.activeUserNames.length > 0) {
          result += `<div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid rgba(255,255,255,0.1); font-size: 12px; color: #78716c;">
            ${item.activeUserNames.join(', ')}
          </div>`
        }

        if (compareType.value === 'mom' && item.monthOnMonth != null) {
          const color = item.monthOnMonth >= 0 ? '#10b981' : '#ef4444'
          const arrow = item.monthOnMonth >= 0 ? '↑' : '↓'
          result += `<div style="margin-top: 8px; color: ${color}; font-size: 12px;">
            环比: ${arrow} ${Math.abs(item.monthOnMonth)}%
          </div>`
        } else if (compareType.value === 'yoy' && item.yearOnYear != null) {
          const color = item.yearOnYear >= 0 ? '#10b981' : '#ef4444'
          const arrow = item.yearOnYear >= 0 ? '↑' : '↓'
          result += `<div style="margin-top: 8px; color: ${color}; font-size: 12px;">
            同比: ${arrow} ${Math.abs(item.yearOnYear)}%
          </div>`
        }

        return result
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: items.map(item => item.date),
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
      axisLabel: { color: '#78716c' }
    },
    yAxis: {
      type: 'value',
      name: '活跃用户数',
      nameTextStyle: { color: '#78716c' },
      axisLine: { show: false },
      axisLabel: { color: '#78716c' },
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } }
    },
    series: [
      {
        name: '活跃用户',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: '#10b981'
        },
        itemStyle: {
          color: '#10b981',
          borderColor: '#0a0a0b',
          borderWidth: 2
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(16, 185, 129, 0.3)' },
              { offset: 1, color: 'rgba(16, 185, 129, 0)' }
            ]
          }
        },
        data: items.map(item => item.activeUsers)
      }
    ]
  }
})

const deviceStatusOption = computed<EChartsOption>(() => {
  if (!deviceStatusData.value) return null

  const data = deviceStatusData.value

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(20, 20, 22, 0.9)',
      borderColor: 'rgba(255, 255, 255, 0.1)',
      textStyle: { color: '#fafaf9' },
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      textStyle: { color: '#78716c' }
    },
    series: [
      {
        name: '设备状态',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#0a0a0b',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          color: '#fafaf9'
        },
        labelLine: {
          lineStyle: { color: 'rgba(255, 255, 255, 0.2)' }
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '1.1rem',
            fontWeight: 'bold'
          }
        },
        data: [
          {
            value: data.onlineCount,
            name: '在线',
            itemStyle: { color: '#10b981' }
          },
          {
            value: data.offlineCount,
            name: '离线',
            itemStyle: { color: '#78716c' }
          },
          {
            value: data.abnormalCount,
            name: '异常',
            itemStyle: { color: '#ef4444' }
          }
        ]
      }
    ]
  }
})

const formatDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const loadUserActivity = async () => {
  loading.value.activity = true
  try {
    const res = await analyticsApi.getUserActivity({
      startDate: dateRange.value.start,
      endDate: dateRange.value.end,
      granularity: granularity.value
    })
    if (res.code === 200) {
      userActivityData.value = res.data
    }
  } catch (error) {
    console.error('Load user activity failed:', error)
    ElMessage.error('加载用户活跃度数据失败')
  } finally {
    loading.value.activity = false
  }
}

const loadDeviceStatus = async () => {
  loading.value.device = true
  try {
    const res = await analyticsApi.getDeviceStatus({
      startDate: dateRange.value.start,
      endDate: dateRange.value.end
    })
    if (res.code === 200) {
      deviceStatusData.value = res.data
    }
  } catch (error) {
    console.error('Load device status failed:', error)
    ElMessage.error('加载设备状态数据失败')
  } finally {
    loading.value.device = false
  }
}

const loadAllData = () => {
  loadUserActivity()
  loadDeviceStatus()
}

const handleTimeRangeChange = (value: string) => {
  timeRange.value = value as any
  if (value !== 'custom') {
    customDateRange.value = null
  }
  loadAllData()
}

const handleGranularityChange = (value: string) => {
  granularity.value = value as any
  loadUserActivity()
}

const handleDevicePieClick = (params: any) => {
  if (params.name) {
    router.push({
      path: '/devices',
      query: { status: params.name.toUpperCase() }
    })
  }
}

onMounted(() => {
  loadAllData()
})
</script>

<style scoped>
@import '../styles/design-system.css';

.ink-analytics-page {
  min-height: 100vh;
  background: var(--ink-black);
}

.ink-page-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: var(--z-fixed);
  background: rgba(10, 10, 11, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--ghost-border);
}

.ink-header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) 0;
}

.ink-header-brand {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.ink-header-icon {
  width: 48px;
  height: 48px;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--forest-400);
}

.ink-header-icon svg {
  width: 24px;
  height: 24px;
}

.ink-page-main {
  padding-top: 120px;
  padding-bottom: var(--space-8);
}

.ink-mt-6 {
  margin-top: var(--space-6);
}

/* Filter Bar */
.ink-filter-bar {
  margin-bottom: var(--space-6);
}

.ink-filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.ink-filter-group {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.ink-filter-label {
  font-size: var(--text-sm);
  color: var(--stone-500);
}

.ink-time-toggle,
.ink-granularity-toggle,
.ink-compare-toggle {
  display: flex;
  background: var(--ink-charcoal);
  border: 1px solid var(--ghost-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.ink-toggle-btn {
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  color: var(--stone-500);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.ink-toggle-sm {
  padding: var(--space-1) var(--space-3);
  font-size: var(--text-xs);
}

.ink-toggle-active {
  background: var(--forest-600);
  color: var(--paper-white);
}

.ink-date-picker {
  width: 240px;
}

:deep(.el-date-editor) {
  background: var(--ink-charcoal) !important;
  border-color: var(--ghost-border) !important;
}

:deep(.el-range-input) {
  background: transparent !important;
  color: var(--paper-white) !important;
}

/* Chart Card */
.ink-chart-card {
  padding: var(--space-5);
}

.ink-chart-lg {
  grid-column: span 2;
}

.ink-chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.ink-chart-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--paper-white);
}

.ink-chart-title svg {
  width: 18px;
  height: 18px;
  color: var(--forest-400);
}

.ink-chart-container {
  height: 300px;
}

.ink-chart {
  width: 100%;
  height: 100%;
}

.ink-chart-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--stone-500);
}

.ink-chart-empty svg {
  width: 48px;
  height: 48px;
  margin-bottom: var(--space-3);
  opacity: 0.5;
}

/* Status Card */
.ink-status-card {
  padding: var(--space-5);
}

.ink-status-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.ink-status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  background: var(--ink-charcoal);
  border-radius: var(--radius-xl);
  border-left: 3px solid transparent;
}

.ink-status-online {
  border-left-color: #10b981;
}

.ink-status-offline {
  border-left-color: #78716c;
}

.ink-status-error {
  border-left-color: #ef4444;
}

.ink-status-indicator {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.ink-status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.ink-status-online .ink-status-dot {
  background: #10b981;
  box-shadow: 0 0 12px #10b981;
}

.ink-status-offline .ink-status-dot {
  background: #78716c;
}

.ink-status-error .ink-status-dot {
  background: #ef4444;
  box-shadow: 0 0 12px #ef4444;
}

.ink-status-name {
  font-weight: 500;
  color: var(--paper-white);
}

.ink-status-numbers {
  text-align: right;
}

.ink-status-count {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: 700;
  color: var(--paper-white);
  display: block;
}

.ink-status-rate {
  font-size: var(--text-sm);
  color: var(--stone-500);
}

/* Loading */
:deep(.el-loading-mask) {
  background: rgba(10, 10, 11, 0.8) !important;
}

/* Responsive */
@media (max-width: 1024px) {
  .ink-chart-lg {
    grid-column: span 1;
  }

  .ink-filter-row {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .ink-time-toggle,
  .ink-granularity-toggle {
    flex-wrap: wrap;
  }

  .ink-toggle-btn {
    flex: 1;
    min-width: 60px;
  }
}
</style>
