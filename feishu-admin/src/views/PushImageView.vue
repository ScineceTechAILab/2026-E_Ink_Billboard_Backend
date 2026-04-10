<template>
  <div class="ink-push-page">
    <!-- Background -->
    <div class="ink-grid-bg" />

    <!-- Header -->
    <header class="ink-page-header">
      <div class="ink-container">
        <div class="ink-header-content">
          <div class="ink-header-brand">
            <div class="ink-header-icon ink-glow-box">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="22" y1="2" x2="11" y2="13" />
                <polygon points="22 2 15 22 11 13 2 9 22 2" />
              </svg>
            </div>
            <div>
              <h1 class="ink-heading ink-heading-4">图片推送</h1>
              <p class="ink-body">选择图片并推送到设备</p>
            </div>
          </div>
          <button @click="goBack" class="ink-btn ink-btn-secondary">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            返回
          </button>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="ink-page-main">
      <div class="ink-container">
        <div class="ink-grid ink-grid-2">
          <!-- Image Selection -->
          <div class="ink-card ink-push-section">
            <h3 class="ink-section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
              </svg>
              选择图片
            </h3>

            <div v-if="imagesLoading" class="ink-loading-inline">
              <div class="ink-spinner" />
              <span>加载图片列表...</span>
            </div>

            <div v-else class="ink-push-content">
              <div class="ink-push-filters">
                <div class="ink-filter-row">
                  <div class="ink-owner-toggle">
                    <button
                      :class="['ink-toggle-btn', { 'ink-toggle-active': imageQuery.owner === 'self' }]"
                      @click="handleOwnerChange('self')"
                    >
                      我的
                    </button>
                    <button
                      :class="['ink-toggle-btn', { 'ink-toggle-active': imageQuery.owner === 'all' }]"
                      @click="handleOwnerChange('all')"
                    >
                      全部
                    </button>
                  </div>

                  <div class="ink-sort-toggle">
                    <button
                      :class="['ink-sort-btn', { 'ink-sort-active': imageQuery.sortOrder === 'desc' }]"
                      @click="handleSortChange('desc')"
                    >
                      最新
                    </button>
                    <button
                      :class="['ink-sort-btn', { 'ink-sort-active': imageQuery.sortOrder === 'asc' }]"
                      @click="handleSortChange('asc')"
                    >
                      最早
                    </button>
                  </div>
                </div>

                <div class="ink-filter-row">
                  <el-select
                    v-model="imageQuery.auditStatus"
                    placeholder="审核状态"
                    clearable
                    class="ink-select-sm"
                    @change="loadImages(true)"
                  >
                    <el-option label="全部" value="" />
                    <el-option label="待审核" value="PENDING" />
                    <el-option label="已通过" value="APPROVED" />
                  </el-select>

                  <el-select
                    v-if="imageQuery.owner === 'all'"
                    v-model="imageQuery.userIds"
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    placeholder="用户"
                    clearable
                    filterable
                    remote
                    :remote-method="remoteUserMethod"
                    :loading="userLoading"
                    class="ink-select-sm"
                    @change="loadImages(true)"
                  >
                    <el-option
                      v-for="user in filteredUserList"
                      :key="user.id"
                      :label="user.nickname"
                      :value="user.id"
                    />
                  </el-select>

                  <div class="ink-search-box ink-search-sm">
                    <svg class="ink-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="11" cy="11" r="8" />
                      <path d="M21 21l-4.35-4.35" />
                    </svg>
                    <input
                      v-model="imageQuery.fileName"
                      @input="handleSearch"
                      type="text"
                      placeholder="搜索..."
                      class="ink-input"
                    />
                  </div>
                </div>
              </div>

              <div class="ink-images-scroll" @scroll="handleScroll">
                <div v-if="images.length > 0" class="ink-images-grid-sm">
                  <div
                    v-for="image in images"
                    :key="image.id"
                    :class="['ink-image-select-card', { 'ink-image-selected': selectedImage?.id === image.id }]"
                    @click="selectImage(image)"
                  >
                    <div class="ink-image-select-preview">
                      <img :src="image.processedUrl || image.originalUrl" :alt="image.fileName" />
                    </div>
                    <p class="ink-image-select-name">{{ image.fileName }}</p>
                    <div class="ink-image-select-meta">
                      <span :class="['ink-badge ink-badge-xs', image.auditStatus === 'APPROVED' ? 'ink-badge-success' : 'ink-badge-warning']">
                        {{ image.auditStatus === 'APPROVED' ? '已通过' : '待审核' }}
                      </span>
                      <span class="ink-image-select-size">{{ formatFileSize(image.fileSize) }}</span>
                    </div>
                  </div>
                </div>

                <div v-else class="ink-empty-state-sm">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                    <circle cx="8.5" cy="8.5" r="1.5" />
                    <polyline points="21 15 16 10 5 21" />
                  </svg>
                  <p>暂无图片</p>
                </div>

                <div v-if="imagesLoading && images.length > 0" class="ink-load-more">
                  <div class="ink-spinner-sm" />
                  <span>加载中...</span>
                </div>

                <div v-if="!hasMoreImages && images.length > 0" class="ink-no-more">
                  没有更多图片了
                </div>
              </div>
            </div>
          </div>

          <!-- Device Selection -->
          <div class="ink-card ink-push-section">
            <h3 class="ink-section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                <line x1="8" y1="21" x2="16" y2="21" />
                <line x1="12" y1="17" x2="12" y2="21" />
              </svg>
              选择设备
            </h3>

            <div v-if="devicesLoading" class="ink-loading-inline">
              <div class="ink-spinner" />
              <span>加载设备列表...</span>
            </div>

            <div v-else class="ink-push-content">
              <div class="ink-push-filters">
                <el-select
                  v-model="deviceStatusFilter"
                  placeholder="设备状态"
                  clearable
                  class="ink-select-sm"
                  @change="loadDevices"
                >
                  <el-option label="全部" value="" />
                  <el-option label="在线" value="ONLINE" />
                  <el-option label="离线" value="OFFLINE" />
                </el-select>

                <div class="ink-search-box ink-search-sm">
                  <svg class="ink-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="11" cy="11" r="8" />
                    <path d="M21 21l-4.35-4.35" />
                  </svg>
                  <input
                    v-model="deviceSearch"
                    type="text"
                    placeholder="搜索设备..."
                    class="ink-input"
                  />
                </div>
              </div>

              <div class="ink-devices-scroll">
                <div v-if="filteredDevices.length > 0" class="ink-device-list">
                  <div
                    v-for="device in filteredDevices"
                    :key="device.id"
                    :class="['ink-device-select-card', { 'ink-device-selected': selectedDeviceIds.includes(device.id) }]"
                    @click="toggleDevice(device)"
                  >
                    <div class="ink-device-select-header">
                      <div class="ink-device-select-info">
                        <h4 class="ink-device-select-name">{{ device.deviceName }}</h4>
                        <p class="ink-device-select-code">{{ device.deviceCode }}</p>
                        <p v-if="device.location" class="ink-device-select-location">
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                            <circle cx="12" cy="10" r="3" />
                          </svg>
                          {{ device.location }}
                        </p>
                      </div>
                      <span :class="['ink-badge ink-badge-xs', device.status === 'ONLINE' ? 'ink-badge-success' : 'ink-badge-warning']">
                        {{ device.status === 'ONLINE' ? '在线' : '离线' }}
                      </span>
                    </div>
                    <div v-if="selectedDeviceIds.includes(device.id)" class="ink-device-selected-indicator">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                    </div>
                  </div>
                </div>

                <div v-else class="ink-empty-state-sm">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                    <line x1="8" y1="21" x2="16" y2="21" />
                    <line x1="12" y1="17" x2="12" y2="21" />
                  </svg>
                  <p>暂无设备</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Preview Section -->
        <div class="ink-card ink-preview-section">
          <h3 class="ink-section-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            推送预览
          </h3>

          <div class="ink-preview-content">
            <div class="ink-preview-item">
              <h4 class="ink-preview-label">选中图片</h4>
              <div v-if="selectedImage" class="ink-preview-image">
                <img :src="selectedImage.processedUrl || selectedImage.originalUrl" :alt="selectedImage.fileName" />
                <div class="ink-preview-image-info">
                  <p class="ink-preview-image-name">{{ selectedImage.fileName }}</p>
                  <p class="ink-preview-image-meta">{{ formatFileSize(selectedImage.fileSize) }}</p>
                </div>
              </div>
              <div v-else class="ink-preview-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <polyline points="21 15 16 10 5 21" />
                </svg>
                <p>请选择图片</p>
              </div>
            </div>

            <div class="ink-preview-item">
              <h4 class="ink-preview-label">
                选中设备 ({{ selectedDeviceIds.length }})
              </h4>
              <div v-if="selectedDeviceIds.length > 0" class="ink-preview-devices">
                <span
                  v-for="deviceId in selectedDeviceIds"
                  :key="deviceId"
                  class="ink-preview-device-tag"
                >
                  {{ getDeviceById(deviceId)?.deviceName }}
                </span>
              </div>
              <div v-else class="ink-preview-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                  <line x1="8" y1="21" x2="16" y2="21" />
                  <line x1="12" y1="17" x2="12" y2="21" />
                </svg>
                <p>请选择设备</p>
              </div>
            </div>
          </div>

          <div class="ink-preview-actions">
            <button @click="resetSelection" class="ink-btn ink-btn-secondary">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 4 23 10 17 10" />
                <path d="M20.49 15a9 9 0 11-2.12-9.36L23 10" />
              </svg>
              重置
            </button>
            <button
              @click="handlePush"
              :disabled="!canPush || pushing"
              class="ink-btn ink-btn-primary ink-btn-lg"
            >
              <span v-if="pushing" class="ink-spinner" />
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="22" y1="2" x2="11" y2="13" />
                <polygon points="22 2 15 22 11 13 2 9 22 2" />
              </svg>
              {{ pushing ? '推送中...' : '推送图片' }}
            </button>
          </div>
        </div>

        <!-- Push Results -->
        <div v-if="pushResults.length > 0" class="ink-card ink-results-section">
          <h3 class="ink-section-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="8" y1="6" x2="21" y2="6" />
              <line x1="8" y1="12" x2="21" y2="12" />
              <line x1="8" y1="18" x2="21" y2="18" />
              <line x1="3" y1="6" x2="3.01" y2="6" />
              <line x1="3" y1="12" x2="3.01" y2="12" />
              <line x1="3" y1="18" x2="3.01" y2="18" />
            </svg>
            推送结果
          </h3>

          <div class="ink-results-list">
            <div
              v-for="result in pushResults"
              :key="result.deviceId"
              :class="['ink-result-item', result.success ? 'ink-result-success' : 'ink-result-error']"
            >
              <div class="ink-result-icon">
                <svg v-if="result.success" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                  <polyline points="22 4 12 14.01 9 11.01" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="15" y1="9" x2="9" y2="15" />
                  <line x1="9" y1="9" x2="15" y2="15" />
                </svg>
              </div>
              <div class="ink-result-info">
                <p class="ink-result-name">{{ result.deviceName }}</p>
                <p v-if="result.error" class="ink-result-error-text">{{ result.error }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { imageApi, deviceApi, pushApi, adminApi } from '@/api'
import type { ImageVO, DeviceVO, UserVO } from '@/types'

const router = useRouter()

const imagesLoading = ref(false)
const devicesLoading = ref(false)
const pushing = ref(false)

const images = ref<ImageVO[]>([])
const devices = ref<DeviceVO[]>([])
const userList = ref<UserVO[]>([])
const filteredUserList = ref<UserVO[]>([])
const userLoading = ref(false)

const imageQuery = reactive({
  current: 1,
  size: 20,
  fileName: '',
  auditStatus: '',
  owner: 'self',
  sortOrder: 'desc',
  userIds: [] as number[]
})
const hasMoreImages = ref(true)

const deviceSearch = ref('')
const deviceStatusFilter = ref('')

const selectedImage = ref<ImageVO | null>(null)
const selectedDeviceIds = ref<number[]>([])
const pushResults = ref<Array<{ deviceId: number; deviceName: string; success: boolean; error?: string }>>([])

const debounceFn = (fn: Function, delay: number) => {
  let timer: any = null
  return (...args: any[]) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}

const filteredDevices = computed(() => {
  return devices.value.filter((device) => {
    const matchesSearch =
      !deviceSearch.value ||
      device.deviceName.toLowerCase().includes(deviceSearch.value.toLowerCase()) ||
      device.deviceCode.toLowerCase().includes(deviceSearch.value.toLowerCase())
    const matchesStatus = !deviceStatusFilter.value || device.status === deviceStatusFilter.value
    return matchesSearch && matchesStatus
  })
})

const canPush = computed(() => {
  return selectedImage.value && selectedDeviceIds.value.length > 0
})

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

const getDeviceById = (deviceId: number): DeviceVO | undefined => {
  return devices.value.find((d) => d.id === deviceId)
}

const loadImages = async (reset = false) => {
  if (reset) {
    imageQuery.current = 1
    images.value = []
    hasMoreImages.value = true
  }
  if (!hasMoreImages.value && !reset) return

  imagesLoading.value = true
  try {
    const res = await imageApi.list({
      current: imageQuery.current,
      size: imageQuery.size,
      fileName: imageQuery.fileName,
      auditStatus: imageQuery.auditStatus || undefined,
      owner: imageQuery.owner as 'self' | 'all',
      sortOrder: imageQuery.sortOrder as 'asc' | 'desc',
      userIds: imageQuery.owner === 'self' ? undefined : imageQuery.userIds
    })
    if (res.code === 200) {
      const newImages = res.data.records
      if (reset) {
        images.value = newImages
      } else {
        images.value.push(...newImages)
      }
      if (images.value.length >= res.data.total) {
        hasMoreImages.value = false
      } else {
        imageQuery.current++
      }
    }
  } catch (error) {
    console.error('Load images failed:', error)
    ElMessage.error('加载图片列表失败')
  } finally {
    imagesLoading.value = false
  }
}

const loadUsers = async () => {
  try {
    const res = await adminApi.getUsers()
    if (res.code === 200) {
      userList.value = res.data
      filteredUserList.value = userList.value.slice(0, 10)
    }
  } catch (error) {
    console.error('Load users failed:', error)
  }
}

const remoteUserMethod = (query: string) => {
  if (query) {
    userLoading.value = true
    setTimeout(() => {
      userLoading.value = false
      filteredUserList.value = userList.value.filter(item => {
        const name = item.nickname || ''
        return name.toLowerCase().includes(query.toLowerCase())
      }).slice(0, 10)
    }, 300)
  } else {
    filteredUserList.value = userList.value.slice(0, 10)
  }
}

const handleSearch = debounceFn(() => {
  loadImages(true)
}, 300)

const handleOwnerChange = (owner: 'self' | 'all') => {
  imageQuery.owner = owner
  imageQuery.userIds = []
  loadImages(true)
}

const handleSortChange = (order: string) => {
  imageQuery.sortOrder = order
  loadImages(true)
}

const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 20) {
    if (hasMoreImages.value && !imagesLoading.value) {
      loadImages()
    }
  }
}

const loadDevices = async () => {
  devicesLoading.value = true
  try {
    const res = await deviceApi.list()
    if (res.code === 200) {
      devices.value = res.data
    }
  } catch (error) {
    console.error('Load devices failed:', error)
    ElMessage.error('加载设备列表失败')
  } finally {
    devicesLoading.value = false
  }
}

const selectImage = (image: ImageVO) => {
  selectedImage.value = image
}

const toggleDevice = (device: DeviceVO) => {
  const index = selectedDeviceIds.value.indexOf(device.id)
  if (index > -1) {
    selectedDeviceIds.value.splice(index, 1)
  } else {
    selectedDeviceIds.value.push(device.id)
  }
}

const resetSelection = () => {
  selectedImage.value = null
  selectedDeviceIds.value = []
  pushResults.value = []
}

const handlePush = async () => {
  if (!selectedImage.value || selectedDeviceIds.value.length === 0) {
    ElMessage.warning('请选择图片和设备')
    return
  }

  pushing.value = true
  pushResults.value = []

  try {
    if (selectedDeviceIds.value.length === 1) {
      await pushApi.pushImage({
        deviceId: selectedDeviceIds.value[0],
        imageId: selectedImage.value.id
      })
      pushResults.value = [
        {
          deviceId: selectedDeviceIds.value[0],
          deviceName: getDeviceById(selectedDeviceIds.value[0])?.deviceName || '',
          success: true
        }
      ]
      ElMessage.success('推送成功')
    } else {
      await pushApi.pushBatch({
        deviceIds: selectedDeviceIds.value,
        contentId: selectedImage.value.id,
        contentType: 'IMAGE'
      })
      pushResults.value = selectedDeviceIds.value.map((deviceId) => ({
        deviceId,
        deviceName: getDeviceById(deviceId)?.deviceName || '',
        success: true
      }))
      ElMessage.success('批量推送成功')
    }
  } catch (error: any) {
    console.error('Push failed:', error)
    if (selectedDeviceIds.value.length === 1) {
      pushResults.value = [
        {
          deviceId: selectedDeviceIds.value[0],
          deviceName: getDeviceById(selectedDeviceIds.value[0])?.deviceName || '',
          success: false,
          error: error.message || '推送失败'
        }
      ]
    } else {
      pushResults.value = selectedDeviceIds.value.map((deviceId) => ({
        deviceId,
        deviceName: getDeviceById(deviceId)?.deviceName || '',
        success: false,
        error: error.message || '推送失败'
      }))
    }
    ElMessage.error(error.message || '推送失败')
  } finally {
    pushing.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadImages(true)
  loadDevices()
  loadUsers()
})
</script>

<style scoped>
@import '../styles/design-system.css';

.ink-push-page {
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

/* Push Section */
.ink-push-section {
  height: 500px;
  display: flex;
  flex-direction: column;
}

.ink-section-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--paper-white);
  margin-bottom: var(--space-4);
  flex-shrink: 0;
}

.ink-section-title svg {
  width: 20px;
  height: 20px;
  color: var(--forest-400);
}

.ink-push-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.ink-loading-inline {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-8);
  color: var(--stone-500);
}

.ink-spinner-sm {
  width: 16px;
  height: 16px;
  border: 2px solid var(--ink-slate);
  border-top-color: var(--forest-500);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Filters */
.ink-push-filters {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
  flex-shrink: 0;
}

.ink-filter-row {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.ink-owner-toggle,
.ink-sort-toggle {
  display: flex;
  background: var(--ink-charcoal);
  border: 1px solid var(--ghost-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.ink-toggle-btn,
.ink-sort-btn {
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-xs);
  color: var(--stone-500);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.ink-toggle-active,
.ink-sort-active {
  background: var(--forest-600);
  color: var(--paper-white);
}

.ink-select-sm {
  width: 110px;
}

.ink-search-sm {
  flex: 1;
  min-width: 120px;
}

.ink-search-sm .ink-input {
  padding-top: var(--space-2);
  padding-bottom: var(--space-2);
  font-size: var(--text-xs);
}

.ink-badge-xs {
  padding: 2px 6px;
  font-size: 10px;
}

/* Scroll Areas */
.ink-images-scroll,
.ink-devices-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

/* Image Grid */
.ink-images-grid-sm {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-2);
}

.ink-image-select-card {
  padding: var(--space-2);
  background: var(--ink-charcoal);
  border: 2px solid transparent;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.ink-image-select-card:hover {
  border-color: var(--ghost-border);
}

.ink-image-selected {
  border-color: var(--forest-500);
  background: rgba(16, 185, 129, 0.1);
}

.ink-image-select-preview {
  aspect-ratio: 1;
  border-radius: var(--radius-md);
  overflow: hidden;
  margin-bottom: var(--space-2);
}

.ink-image-select-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ink-image-select-name {
  font-size: var(--text-xs);
  color: var(--paper-white);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: var(--space-1);
}

.ink-image-select-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ink-image-select-size {
  font-size: 10px;
  color: var(--stone-500);
}

/* Device List */
.ink-device-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.ink-device-select-card {
  position: relative;
  padding: var(--space-3);
  background: var(--ink-charcoal);
  border: 2px solid transparent;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.ink-device-select-card:hover {
  border-color: var(--ghost-border);
}

.ink-device-selected {
  border-color: var(--forest-500);
  background: rgba(16, 185, 129, 0.1);
}

.ink-device-select-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-2);
}

.ink-device-select-info {
  flex: 1;
  min-width: 0;
}

.ink-device-select-name {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--paper-white);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ink-device-select-code {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  color: var(--stone-500);
  margin-top: 2px;
}

.ink-device-select-location {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--stone-400);
  margin-top: var(--space-1);
}

.ink-device-select-location svg {
  width: 12px;
  height: 12px;
}

.ink-device-selected-indicator {
  position: absolute;
  bottom: var(--space-2);
  right: var(--space-2);
  width: 20px;
  height: 20px;
  background: var(--forest-500);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--paper-white);
}

.ink-device-selected-indicator svg {
  width: 12px;
  height: 12px;
}

/* Empty State */
.ink-empty-state-sm {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-8);
  color: var(--stone-500);
}

.ink-empty-state-sm svg {
  width: 40px;
  height: 40px;
  margin-bottom: var(--space-2);
  opacity: 0.5;
}

.ink-empty-state-sm p {
  font-size: var(--text-sm);
}

.ink-load-more,
.ink-no-more {
  text-align: center;
  padding: var(--space-3);
  font-size: var(--text-xs);
  color: var(--stone-500);
}

/* Preview Section */
.ink-preview-section {
  margin-top: var(--space-6);
}

.ink-preview-content {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-6);
  margin-top: var(--space-4);
}

.ink-preview-item {
  min-height: 120px;
}

.ink-preview-label {
  font-size: var(--text-sm);
  color: var(--stone-500);
  margin-bottom: var(--space-3);
}

.ink-preview-image {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--ink-charcoal);
  border-radius: var(--radius-lg);
}

.ink-preview-image img {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-md);
  object-fit: cover;
}

.ink-preview-image-info {
  flex: 1;
  min-width: 0;
}

.ink-preview-image-name {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--paper-white);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ink-preview-image-meta {
  font-size: var(--text-xs);
  color: var(--stone-500);
  margin-top: 2px;
}

.ink-preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
  background: var(--ink-charcoal);
  border-radius: var(--radius-lg);
  color: var(--stone-500);
}

.ink-preview-empty svg {
  width: 32px;
  height: 32px;
  margin-bottom: var(--space-2);
  opacity: 0.5;
}

.ink-preview-devices {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.ink-preview-device-tag {
  padding: var(--space-2) var(--space-3);
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  color: var(--forest-400);
}

.ink-preview-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-6);
  padding-top: var(--space-6);
  border-top: 1px solid var(--ghost-border);
}

.ink-btn-lg {
  padding: var(--space-3) var(--space-6);
}

/* Results Section */
.ink-results-section {
  margin-top: var(--space-6);
}

.ink-results-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.ink-result-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-lg);
}

.ink-result-success {
  background: rgba(16, 185, 129, 0.1);
  border-left: 3px solid var(--forest-500);
}

.ink-result-error {
  background: rgba(239, 68, 68, 0.1);
  border-left: 3px solid #ef4444;
}

.ink-result-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.ink-result-success .ink-result-icon {
  color: var(--forest-400);
}

.ink-result-error .ink-result-icon {
  color: #f87171;
}

.ink-result-name {
  font-weight: 500;
  color: var(--paper-white);
}

.ink-result-error-text {
  font-size: var(--text-sm);
  color: #f87171;
  margin-top: 2px;
}

/* Responsive */
@media (max-width: 768px) {
  .ink-push-section {
    height: auto;
    max-height: 400px;
  }

  .ink-preview-content {
    grid-template-columns: 1fr;
  }

  .ink-images-grid-sm {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
