<template>
  <div class="min-h-screen p-6">
    <div class="max-w-7xl mx-auto space-y-6">
      
      <!-- 页面标题 -->
      <div class="card animate-slide-up">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-800 mb-1">
              📊 数据统计
            </h1>
            <p class="text-gray-500">洞察业务数据，做出明智决策</p>
          </div>
          <button
            @click="router.push('/dashboard')"
            class="px-4 py-2 bg-white border border-gray-300 rounded-lg shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
          >
            <span class="mr-2">🔙</span>
            返回仪表台
          </button>
        </div>
      </div>

      <!-- 时间筛选器 -->
      <div class="card animate-slide-up" style="animation-delay: 0.1s">
        <div class="flex flex-wrap items-center gap-4">
          <span class="text-gray-600 font-medium">时间范围：</span>
          <el-radio-group v-model="timeRange" @change="handleTimeRangeChange">
            <el-radio-button value="7d">最近 7 天</el-radio-button>
            <el-radio-button value="30d">最近 30 天</el-radio-button>
            <el-radio-button value="90d">最近 90 天</el-radio-button>
            <el-radio-button value="custom">自定义</el-radio-button>
          </el-radio-group>
          
          <div v-if="timeRange === 'custom'" class="flex items-center gap-2">
            <el-date-picker
              v-model="customDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :shortcuts="dateShortcuts"
              :disabled-date="disabledDate"
              @change="loadAllData"
            />
          </div>
          
          <div class="ml-auto flex items-center gap-2">
            <span class="text-gray-600 font-medium">时间粒度：</span>
            <el-radio-group v-model="granularity" size="small" @change="loadUserActivity">
              <el-radio-button value="day">日</el-radio-button>
              <el-radio-button value="week">周</el-radio-button>
              <el-radio-button value="month">月</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </div>

      <!-- 概览统计卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div class="card animate-slide-up" style="animation-delay: 0.2s">
          <div class="flex items-center">
            <div class="w-14 h-14 bg-gradient-to-r from-blue-400 to-blue-500 rounded-xl flex items-center justify-center text-white text-2xl mr-4">
              👥
            </div>
            <div>
              <p class="text-gray-500 text-sm">活跃用户</p>
              <p class="text-2xl font-bold text-gray-800">{{ userActivityData?.totalActiveUsers || 0 }}</p>
            </div>
          </div>
        </div>

        <div class="card animate-slide-up" style="animation-delay: 0.3s">
          <div class="flex items-center">
            <div class="w-14 h-14 bg-gradient-to-r from-green-400 to-green-500 rounded-xl flex items-center justify-center text-white text-2xl mr-4">
              📈
            </div>
            <div>
              <p class="text-gray-500 text-sm">日均活跃</p>
              <p class="text-2xl font-bold text-gray-800">{{ userActivityData?.avgDailyActiveUsers || 0 }}</p>
            </div>
          </div>
        </div>

        <div class="card animate-slide-up" style="animation-delay: 0.4s">
          <div class="flex items-center">
            <div class="w-14 h-14 bg-gradient-to-r from-purple-400 to-purple-500 rounded-xl flex items-center justify-center text-white text-2xl mr-4">
              📺
            </div>
            <div>
              <p class="text-gray-500 text-sm">在线设备</p>
              <p class="text-2xl font-bold text-gray-800">{{ deviceStatusData?.onlineCount || 0 }}</p>
            </div>
          </div>
        </div>

        <div class="card animate-slide-up" style="animation-delay: 0.5s">
          <div class="flex items-center">
            <div class="w-14 h-14 bg-gradient-to-r from-orange-400 to-orange-500 rounded-xl flex items-center justify-center text-white text-2xl mr-4">
              📊
            </div>
            <div>
              <p class="text-gray-500 text-sm">在线率</p>
              <p class="text-2xl font-bold text-gray-800">{{ deviceStatusData?.onlineRate || 0 }}%</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        <!-- 用户活跃度趋势图 -->
        <div class="card animate-slide-up lg:col-span-2" style="animation-delay: 0.6s">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold text-gray-800">
              <span class="mr-2">📈</span>
              用户活跃度趋势
            </h2>
            <el-radio-group v-model="compareType" size="small">
              <el-radio-button value="mom">环比</el-radio-button>
              <el-radio-button value="yoy">同比</el-radio-button>
            </el-radio-group>
          </div>
          <div v-loading="loading.activity" class="h-80">
            <v-chart
              v-if="userActivityOption"
              :option="userActivityOption"
              class="w-full h-full"
              autoresize
            />
            <el-empty v-else description="暂无数据" />
          </div>
        </div>

        <!-- 设备在线状态饼图 -->
        <div class="card animate-slide-up" style="animation-delay: 0.7s">
          <h2 class="text-lg font-semibold text-gray-800 mb-4">
            <span class="mr-2">🎯</span>
            设备状态分布
          </h2>
          <div v-loading="loading.device" class="h-80">
            <v-chart
              v-if="deviceStatusOption"
              :option="deviceStatusOption"
              class="w-full h-full"
              autoresize
              @click="handleDevicePieClick"
            />
            <el-empty v-else description="暂无数据" />
          </div>
        </div>

        <!-- 设备状态详情 -->
        <div class="card animate-slide-up" style="animation-delay: 0.8s">
          <h2 class="text-lg font-semibold text-gray-800 mb-4">
            <span class="mr-2">📋</span>
            设备状态详情
          </h2>
          <div class="space-y-4">
            <div class="flex items-center justify-between p-4 bg-green-50 rounded-xl">
              <div class="flex items-center">
                <div class="w-4 h-4 bg-green-500 rounded-full mr-3"></div>
                <span class="text-gray-700">在线</span>
              </div>
              <div class="text-right">
                <p class="text-xl font-bold text-green-600">{{ deviceStatusData?.onlineCount || 0 }}</p>
                <p class="text-sm text-gray-500">{{ deviceStatusData?.onlineRate || 0 }}%</p>
              </div>
            </div>
            
            <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
              <div class="flex items-center">
                <div class="w-4 h-4 bg-gray-400 rounded-full mr-3"></div>
                <span class="text-gray-700">离线</span>
              </div>
              <div class="text-right">
                <p class="text-xl font-bold text-gray-600">{{ deviceStatusData?.offlineCount || 0 }}</p>
                <p class="text-sm text-gray-500">{{ deviceStatusData?.offlineRate || 0 }}%</p>
              </div>
            </div>
            
            <div class="flex items-center justify-between p-4 bg-red-50 rounded-xl">
              <div class="flex items-center">
                <div class="w-4 h-4 bg-red-500 rounded-full mr-3"></div>
                <span class="text-gray-700">异常</span>
              </div>
              <div class="text-right">
                <p class="text-xl font-bold text-red-600">{{ deviceStatusData?.abnormalCount || 0 }}</p>
                <p class="text-sm text-gray-500">{{ deviceStatusData?.abnormalRate || 0 }}%</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
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

// ==================== 状态管理 ====================
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

// ==================== 日期快捷选项 ====================
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

// ==================== 日期限制（最多90天） ====================
const disabledDate = (time: Date) => {
  const ninetyDaysAgo = new Date()
  ninetyDaysAgo.setTime(ninetyDaysAgo.getTime() - 3600 * 1000 * 24 * 90)
  return time.getTime() > Date.now() || time.getTime() < ninetyDaysAgo.getTime()
}

// ==================== 计算日期范围 ====================
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

// ==================== 图表配置 ====================
const userActivityOption = computed<EChartsOption>(() => {
  if (!userActivityData.value?.items.length) return null
  
  const items = userActivityData.value.items
  
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = items[params[0].dataIndex]
        let result = `${params[0].axisValue}<br/>`
        result += `活跃用户: ${item.activeUsers}<br/>`
        
        // 展示活跃用户名单
        if (item.activeUserNames && item.activeUserNames.length > 0) {
          result += `<span style="font-size:12px;color:#cbd5e1;display:block;margin-top:4px;max-width:200px;white-space:normal;line-height:1.4">(${item.activeUserNames.join(', ')})</span>`
        }
        
        if (compareType.value === 'mom' && item.monthOnMonth != null) {
          const color = item.monthOnMonth >= 0 ? '#10b981' : '#ef4444'
          const arrow = item.monthOnMonth >= 0 ? '↑' : '↓'
          result += `<br/><span style="color:${color}">环比: ${arrow} ${Math.abs(item.monthOnMonth)}%</span>`
        } else if (compareType.value === 'yoy' && item.yearOnYear != null) {
          const color = item.yearOnYear >= 0 ? '#10b981' : '#ef4444'
          const arrow = item.yearOnYear >= 0 ? '↑' : '↓'
          result += `<br/><span style="color:${color}">同比: ${arrow} ${Math.abs(item.yearOnYear)}%</span>`
        }
        
        return result
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: items.map(item => item.date)
    },
    yAxis: {
      type: 'value',
      name: '活跃用户数'
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
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: '#f59e0b' },
              { offset: 1, color: '#ef4444' }
            ]
          }
        },
        itemStyle: {
          color: '#f59e0b'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(245, 158, 11, 0.3)' },
              { offset: 1, color: 'rgba(245, 158, 11, 0.05)' }
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
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '设备状态',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '1.25rem',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: true
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
            itemStyle: { color: '#9ca3af' }
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

// ==================== 方法 ====================
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

const handleTimeRangeChange = () => {
  if (timeRange.value !== 'custom') {
    customDateRange.value = null
  }
  loadAllData()
}

const handleDevicePieClick = (params: any) => {
  if (params.name) {
    router.push({
      path: '/devices',
      query: { status: params.name.toUpperCase() }
    })
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadAllData()
})
</script>

<style scoped>
/* 动画效果 */
.animate-slide-up {
  animation: slideUp 0.5s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
