<template>
  <div class="ink-device-page">
    <!-- Background -->
    <div class="ink-grid-bg" />

    <!-- Header -->
    <header class="ink-page-header">
      <div class="ink-container">
        <div class="ink-header-content">
          <div class="ink-header-brand">
            <div class="ink-header-icon ink-glow-box">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                <line x1="8" y1="21" x2="16" y2="21" />
                <line x1="12" y1="17" x2="12" y2="21" />
              </svg>
            </div>
            <div>
              <h1 class="ink-heading ink-heading-4">设备管理</h1>
              <p class="ink-body">管理所有墨水屏设备</p>
            </div>
          </div>
          <div class="ink-header-actions">
            <button @click="showAddDialog" class="ink-btn ink-btn-primary">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              添加设备
            </button>
            <button @click="goBack" class="ink-btn ink-btn-secondary">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 12H5M12 19l-7-7 7-7" />
              </svg>
              返回
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="ink-page-main">
      <div class="ink-container">
        <!-- Filter Bar -->
        <div class="ink-card ink-filter-bar">
          <div class="ink-filter-group">
            <div class="ink-search-box">
              <svg class="ink-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8" />
                <path d="M21 21l-4.35-4.35" />
              </svg>
              <input
                v-model="searchKeyword"
                type="text"
                placeholder="搜索设备名称或编码..."
                class="ink-input"
                @input="handleSearch"
              />
            </div>

            <el-select
              v-model="statusFilter"
              placeholder="筛选状态"
              clearable
              class="ink-select"
              @change="loadDevices"
            >
              <el-option label="全部" value="" />
              <el-option label="在线" value="ONLINE" />
              <el-option label="离线" value="OFFLINE" />
            </el-select>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="ink-loading-state">
          <div class="ink-spinner-lg" />
          <p>加载设备列表...</p>
        </div>

        <!-- Empty State -->
        <div v-else-if="filteredDevices.length === 0" class="ink-card ink-empty-state">
          <div class="ink-empty-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
              <line x1="8" y1="21" x2="16" y2="21" />
              <line x1="12" y1="17" x2="12" y2="21" />
            </svg>
          </div>
          <h3 class="ink-empty-title">暂无设备</h3>
          <p class="ink-empty-text">开始添加您的第一台墨水屏设备</p>
          <button @click="showAddDialog" class="ink-btn ink-btn-primary ink-mt-4">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            添加第一个设备
          </button>
        </div>

        <!-- Device Grid -->
        <div v-else class="ink-device-grid">
          <div
            v-for="device in filteredDevices"
            :key="device.id"
            class="ink-device-card"
          >
            <div class="ink-device-header">
              <div class="ink-device-status" :class="`ink-status-${device.status.toLowerCase()}`">
                <span class="ink-status-dot" />
              </div>
              <div class="ink-device-title">
                <h3 class="ink-device-name">{{ device.deviceName }}</h3>
                <p class="ink-device-code">{{ device.deviceCode }}</p>
              </div>
              <span :class="['ink-badge', device.status === 'ONLINE' ? 'ink-badge-success' : 'ink-badge-warning']">
                {{ device.status === 'ONLINE' ? '在线' : '离线' }}
              </span>
            </div>

            <div class="ink-device-info">
              <div v-if="device.location" class="ink-info-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                  <circle cx="12" cy="10" r="3" />
                </svg>
                <span>{{ device.location }}</span>
              </div>
              <div v-if="device.description" class="ink-info-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="16" x2="12" y2="12" />
                  <line x1="12" y1="8" x2="12.01" y2="8" />
                </svg>
                <span>{{ device.description }}</span>
              </div>
              <div class="ink-info-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
                <span>最后心跳: {{ formatDate(device.lastHeartbeat) }}</span>
              </div>
            </div>

            <div v-if="device.currentContent" class="ink-device-content">
              <div class="ink-content-preview">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <polyline points="21 15 16 10 5 21" />
                </svg>
                <span>正在播放</span>
              </div>
              <p class="ink-content-name">{{ device.currentContent.fileName }}</p>
            </div>

            <div class="ink-device-actions">
              <button @click="viewDevice(device)" class="ink-action-btn ink-tooltip" data-tooltip="查看详情">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              </button>
              <button @click="editDevice(device)" class="ink-action-btn ink-tooltip" data-tooltip="编辑">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                </svg>
              </button>
              <button @click="showNetworkDialog(device)" class="ink-action-btn ink-tooltip" data-tooltip="更换网络">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M5 12.55a11 11 0 0114.08 0M1.42 9a16 16 0 0121.16 0M8.53 16.11a6 6 0 016.95 0" />
                  <line x1="12" y1="20" x2="12.01" y2="20" />
                </svg>
              </button>
              <button @click="confirmDelete(device)" class="ink-action-btn ink-action-btn-danger ink-tooltip" data-tooltip="删除">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Add/Edit Device Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingDevice ? '编辑设备' : '添加设备'"
      width="500px"
      :close-on-click-modal="false"
      class="ink-dialog"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="ink-form">
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="form.deviceName" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="设备编码" prop="deviceCode">
          <el-input
            v-model="form.deviceCode"
            placeholder="请输入设备编码"
            :disabled="!!editingDevice"
          />
        </el-form-item>
        <el-form-item label="MQTT主题">
          <el-input v-model="form.mqttTopic" placeholder="不填则自动生成" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" placeholder="请输入位置" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <button @click="dialogVisible = false" class="ink-btn ink-btn-secondary">取消</button>
        <button @click="handleSubmit" :disabled="submitting" class="ink-btn ink-btn-primary">
          <span v-if="submitting" class="ink-spinner" />
          {{ editingDevice ? '保存' : '添加' }}
        </button>
      </template>
    </el-dialog>

    <!-- Device Detail Dialog -->
    <el-dialog v-model="detailVisible" title="设备详情" width="600px" class="ink-dialog">
      <div v-if="selectedDevice" class="ink-detail-content">
        <div class="ink-detail-grid">
          <div class="ink-detail-item">
            <span class="ink-detail-label">设备名称</span>
            <span class="ink-detail-value">{{ selectedDevice.deviceName }}</span>
          </div>
          <div class="ink-detail-item">
            <span class="ink-detail-label">设备编码</span>
            <span class="ink-detail-value ink-mono">{{ selectedDevice.deviceCode }}</span>
          </div>
          <div class="ink-detail-item">
            <span class="ink-detail-label">状态</span>
            <span :class="['ink-badge', selectedDevice.status === 'ONLINE' ? 'ink-badge-success' : 'ink-badge-warning']">
              {{ selectedDevice.status === 'ONLINE' ? '在线' : '离线' }}
            </span>
          </div>
          <div class="ink-detail-item">
            <span class="ink-detail-label">MQTT主题</span>
            <span class="ink-detail-value ink-mono">{{ selectedDevice.mqttTopic }}</span>
          </div>
        </div>

        <div v-if="selectedDevice.location" class="ink-detail-row">
          <span class="ink-detail-label">位置</span>
          <span class="ink-detail-value">{{ selectedDevice.location }}</span>
        </div>

        <div v-if="selectedDevice.description" class="ink-detail-row">
          <span class="ink-detail-label">描述</span>
          <span class="ink-detail-value">{{ selectedDevice.description }}</span>
        </div>

        <div class="ink-detail-row">
          <span class="ink-detail-label">最后心跳</span>
          <span class="ink-detail-value">{{ formatDate(selectedDevice.lastHeartbeat) }}</span>
        </div>

        <div class="ink-detail-row">
          <span class="ink-detail-label">创建时间</span>
          <span class="ink-detail-value">{{ formatDate(selectedDevice.createTime) }}</span>
        </div>

        <div v-if="selectedDevice.currentContent" class="ink-detail-content-card">
          <div class="ink-content-preview-lg">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
            <div>
              <p class="ink-content-label">当前播放内容</p>
              <p class="ink-content-value">{{ selectedDevice.currentContent.fileName }}</p>
              <p class="ink-content-meta">{{ formatFileSize(selectedDevice.currentContent.fileSize) }}</p>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <button @click="detailVisible = false" class="ink-btn ink-btn-secondary">关闭</button>
      </template>
    </el-dialog>

    <!-- Network Config Dialog -->
    <el-dialog
      v-model="networkDialogVisible"
      :title="`更换网络 - ${selectedNetworkDevice?.deviceName}`"
      width="500px"
      :close-on-click-modal="false"
      class="ink-dialog"
    >
      <el-form :model="networkForm" :rules="networkRules" ref="networkFormRef" label-width="100px" class="ink-form">
        <el-form-item label="WiFi名称" prop="ssid">
          <el-input v-model="networkForm.ssid" placeholder="请输入WiFi名称" />
        </el-form-item>
        <el-form-item label="WiFi密码" prop="password">
          <el-input
            v-model="networkForm.password"
            type="password"
            show-password
            placeholder="请输入WiFi密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <button @click="networkDialogVisible = false" class="ink-btn ink-btn-secondary">取消</button>
        <button @click="handleChangeNetwork" :disabled="changingNetwork" class="ink-btn ink-btn-primary">
          <span v-if="changingNetwork" class="ink-spinner" />
          {{ changingNetwork ? '发送中...' : '更换网络' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { deviceApi } from '@/api'
import type { DeviceVO, DeviceDTO } from '@/types'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const devices = ref<DeviceVO[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const networkDialogVisible = ref(false)
const editingDevice = ref<DeviceVO | null>(null)
const selectedDevice = ref<DeviceVO | null>(null)
const selectedNetworkDevice = ref<DeviceVO | null>(null)
const formRef = ref<FormInstance>()
const networkFormRef = ref<FormInstance>()
const changingNetwork = ref(false)

const form = reactive<DeviceDTO>({
  deviceName: '',
  deviceCode: '',
  mqttTopic: '',
  location: '',
  description: ''
})

const rules: FormRules = {
  deviceName: [
    { required: true, message: '请输入设备名称', trigger: 'blur' },
    { min: 2, max: 50, message: '设备名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  deviceCode: [
    { required: true, message: '请输入设备编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: '设备编码只能包含字母、数字、下划线和短横线', trigger: 'blur' }
  ]
}

const networkForm = reactive({
  ssid: '',
  password: ''
})

const networkRules: FormRules = {
  ssid: [
    { required: true, message: '请输入WiFi名称', trigger: 'blur' },
    { min: 1, max: 32, message: 'WiFi名称长度在 1 到 32 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入WiFi密码', trigger: 'blur' },
    { min: 8, max: 63, message: 'WiFi密码长度在 8 到 63 个字符', trigger: 'blur' }
  ]
}

const filteredDevices = computed(() => {
  return devices.value.filter((device) => {
    const matchesSearch =
      !searchKeyword.value ||
      device.deviceName.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      device.deviceCode.toLowerCase().includes(searchKeyword.value.toLowerCase())
    const matchesStatus = !statusFilter.value || device.status === statusFilter.value
    return matchesSearch && matchesStatus
  })
})

const formatDate = (dateStr: string): string => {
  return new Date(dateStr).toLocaleString('zh-CN')
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

const loadDevices = async () => {
  loading.value = true
  try {
    const res = await deviceApi.list()
    if (res.code === 200) {
      devices.value = res.data
    }
  } catch (error) {
    console.error('Load devices failed:', error)
    ElMessage.error('加载设备列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {}

const showAddDialog = () => {
  editingDevice.value = null
  Object.assign(form, {
    deviceName: '',
    deviceCode: '',
    mqttTopic: '',
    location: '',
    description: ''
  })
  dialogVisible.value = true
}

const editDevice = (device: DeviceVO) => {
  editingDevice.value = device
  Object.assign(form, {
    deviceName: device.deviceName,
    deviceCode: device.deviceCode,
    mqttTopic: device.mqttTopic || '',
    location: device.location || '',
    description: device.description || ''
  })
  dialogVisible.value = true
}

const viewDevice = (device: DeviceVO) => {
  selectedDevice.value = device
  detailVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (editingDevice.value) {
        await deviceApi.update(editingDevice.value.id, form)
        ElMessage.success('更新成功')
      } else {
        await deviceApi.create(form)
        ElMessage.success('添加成功')
      }
      dialogVisible.value = false
      await loadDevices()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const confirmDelete = (device: DeviceVO) => {
  ElMessageBox.confirm(`确定要删除设备"${device.deviceName}"吗？此操作不可恢复！`, '确认删除', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await deviceApi.delete(device.id)
        ElMessage.success('删除成功')
        await loadDevices()
      } catch (error: any) {
        ElMessage.error(error.message || '删除失败')
      }
    })
    .catch(() => {})
}

const showNetworkDialog = (device: DeviceVO) => {
  selectedNetworkDevice.value = device
  Object.assign(networkForm, {
    ssid: '',
    password: ''
  })
  networkDialogVisible.value = true
}

const handleChangeNetwork = async () => {
  if (!networkFormRef.value || !selectedNetworkDevice.value) return

  await networkFormRef.value.validate(async (valid) => {
    if (!valid) return

    changingNetwork.value = true
    try {
      await deviceApi.changeNetwork(selectedNetworkDevice.value.id, {
        ssid: networkForm.ssid,
        password: networkForm.password
      })
      ElMessage.success('配网指令已下发')
      networkDialogVisible.value = false
    } catch (error: any) {
      ElMessage.error(error.message || '配网失败')
    } finally {
      changingNetwork.value = false
    }
  })
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadDevices()
})
</script>

<style scoped>
@import '../styles/design-system.css';

.ink-device-page {
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

.ink-header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.ink-page-main {
  padding-top: 120px;
  padding-bottom: var(--space-8);
}

.ink-mt-4 {
  margin-top: var(--space-4);
}

/* Filter Bar */
.ink-filter-bar {
  margin-bottom: var(--space-6);
}

.ink-filter-group {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.ink-search-box {
  position: relative;
  flex: 1;
  min-width: 280px;
}

.ink-search-icon {
  position: absolute;
  left: var(--space-3);
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  color: var(--stone-500);
  pointer-events: none;
}

.ink-search-box .ink-input {
  padding-left: var(--space-10);
}

.ink-select {
  width: 140px;
}

/* Loading State */
.ink-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16);
  color: var(--stone-500);
}

.ink-spinner-lg {
  width: 48px;
  height: 48px;
  border: 3px solid var(--ink-slate);
  border-top-color: var(--forest-500);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: var(--space-4);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Empty State */
.ink-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16);
  text-align: center;
}

.ink-empty-icon {
  width: 80px;
  height: 80px;
  color: var(--stone-700);
  margin-bottom: var(--space-4);
}

.ink-empty-title {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--paper-white);
  margin-bottom: var(--space-2);
}

.ink-empty-text {
  color: var(--stone-500);
}

/* Device Grid */
.ink-device-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--space-4);
}

.ink-device-card {
  background: linear-gradient(145deg, var(--ink-graphite) 0%, var(--ink-charcoal) 100%);
  border: 1px solid var(--ghost-border);
  border-radius: var(--radius-2xl);
  padding: var(--space-5);
  transition: all var(--transition-base);
  position: relative;
  overflow: hidden;
}

.ink-device-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--forest-500), transparent);
  opacity: 0;
  transition: opacity var(--transition-base);
}

.ink-device-card:hover::before {
  opacity: 1;
}

.ink-device-card:hover {
  transform: translateY(-4px);
  border-color: rgba(16, 185, 129, 0.2);
  box-shadow: var(--shadow-lg);
}

.ink-device-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.ink-device-status {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: var(--space-1);
}

.ink-status-online {
  background: var(--forest-500);
  box-shadow: 0 0 12px var(--forest-500);
}

.ink-status-offline {
  background: var(--stone-600);
}

.ink-status-dot {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.ink-device-title {
  flex: 1;
  min-width: 0;
}

.ink-device-name {
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--paper-white);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ink-device-code {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  color: var(--stone-500);
  margin-top: var(--space-1);
}

.ink-device-info {
  margin-bottom: var(--space-4);
}

.ink-info-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) 0;
  color: var(--stone-300);
  font-size: var(--text-sm);
}

.ink-info-item svg {
  width: 16px;
  height: 16px;
  color: var(--forest-400);
  flex-shrink: 0;
}

.ink-info-item span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ink-device-content {
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.15);
  border-radius: var(--radius-lg);
  padding: var(--space-3);
  margin-bottom: var(--space-4);
}

.ink-content-preview {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--forest-400);
  font-size: var(--text-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: var(--space-2);
}

.ink-content-preview svg {
  width: 16px;
  height: 16px;
}

.ink-content-name {
  font-size: var(--text-sm);
  color: var(--paper-white);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ink-device-actions {
  display: flex;
  gap: var(--space-2);
  padding-top: var(--space-4);
  border-top: 1px solid var(--ghost-border);
}

.ink-action-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  background: var(--ink-slate);
  border: 1px solid var(--ghost-border);
  color: var(--stone-300);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.ink-action-btn:hover {
  background: var(--stone-800);
  color: var(--paper-white);
  transform: scale(1.05);
}

.ink-action-btn svg {
  width: 16px;
  height: 16px;
}

.ink-action-btn-danger:hover {
  background: rgba(220, 38, 38, 0.2);
  border-color: rgba(220, 38, 38, 0.3);
  color: #f87171;
}

/* Detail Dialog Content */
.ink-detail-content {
  padding: var(--space-2);
}

.ink-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.ink-detail-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.ink-detail-row {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  margin-bottom: var(--space-3);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--ghost-border);
}

.ink-detail-label {
  font-size: var(--text-xs);
  color: var(--stone-500);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.ink-detail-value {
  font-size: var(--text-sm);
  color: var(--paper-white);
}

.ink-detail-content-card {
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.15);
  border-radius: var(--radius-xl);
  padding: var(--space-4);
  margin-top: var(--space-4);
}

.ink-content-preview-lg {
  display: flex;
  gap: var(--space-4);
}

.ink-content-preview-lg svg {
  width: 48px;
  height: 48px;
  color: var(--forest-400);
  flex-shrink: 0;
}

.ink-content-label {
  font-size: var(--text-xs);
  color: var(--forest-400);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.ink-content-value {
  font-size: var(--text-base);
  color: var(--paper-white);
  font-weight: 500;
  margin-top: var(--space-1);
}

.ink-content-meta {
  font-size: var(--text-sm);
  color: var(--stone-500);
  margin-top: var(--space-1);
}

/* Dialog Styles */
:deep(.ink-dialog) {
  background: var(--ink-graphite);
  border-radius: var(--radius-2xl);
  border: 1px solid var(--ghost-border);
}

:deep(.ink-dialog .el-dialog__header) {
  padding: var(--space-6);
  border-bottom: 1px solid var(--ghost-border);
}

:deep(.ink-dialog .el-dialog__title) {
  color: var(--paper-white);
  font-family: var(--font-display);
  font-weight: 600;
}

:deep(.ink-dialog .el-dialog__body) {
  padding: var(--space-6);
  color: var(--ghost-text);
}

:deep(.ink-dialog .el-dialog__footer) {
  padding: var(--space-6);
  border-top: 1px solid var(--ghost-border);
}

:deep(.ink-form .el-form-item__label) {
  color: var(--stone-300);
}

:deep(.ink-form .el-input__inner) {
  background: var(--ink-charcoal);
  border-color: var(--ghost-border);
  color: var(--paper-white);
}

:deep(.ink-form .el-input__inner:focus) {
  border-color: var(--forest-500);
}

/* Responsive */
@media (max-width: 768px) {
  .ink-header-content {
    flex-direction: column;
    gap: var(--space-4);
    align-items: flex-start;
  }

  .ink-device-grid {
    grid-template-columns: 1fr;
  }

  .ink-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
