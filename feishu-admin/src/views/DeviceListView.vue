<template>
  <div class="min-h-screen p-6">
    <div class="max-w-7xl mx-auto space-y-6">
      <div class="card animate-slide-up">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-800 mb-1">
              <i class="fas fa-tv mr-2 text-amber-500"></i>
              设备管理
            </h1>
            <p class="text-gray-500">管理所有墨水屏设备</p>
          </div>
          <div class="flex items-center gap-3">
            <div class="relative">
              <input
                v-model="searchKeyword"
                type="text"
                placeholder="搜索设备名称或编码..."
                class="input-field pl-10 pr-4 py-2 w-64"
                @input="handleSearch"
              />
              <i
                class="fas fa-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
              ></i>
            </div>
            <el-select
              v-model="statusFilter"
              placeholder="筛选状态"
              clearable
              style="width: 150px"
              @change="loadDevices"
            >
              <el-option label="全部" value="" />
              <el-option label="在线" value="ONLINE" />
              <el-option label="离线" value="OFFLINE" />
            </el-select>
            <button @click="showAddDialog" class="btn-primary">
              <i class="fas fa-plus mr-2"></i>
              添加设备
            </button>
            <button @click="goBack" class="btn-secondary">
              <i class="fas fa-arrow-left mr-2"></i>
              返回
            </button>
          </div>
        </div>
      </div>

      <div class="card animate-slide-up" style="animation-delay: 0.1s">
        <div v-if="loading" class="flex items-center justify-center py-12">
          <el-icon class="is-loading text-4xl text-amber-500"><Loading /></el-icon>
          <span class="ml-3 text-gray-500">加载中...</span>
        </div>

        <div v-else-if="filteredDevices.length === 0" class="text-center py-12">
          <div class="text-6xl mb-4">📺</div>
          <p class="text-gray-500 mb-4">暂无设备</p>
          <button @click="showAddDialog" class="btn-primary">
            <i class="fas fa-plus mr-2"></i>
            添加第一个设备
          </button>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div
            v-for="device in filteredDevices"
            :key="device.id"
            class="bg-white rounded-xl p-5 border-2 border-gray-100 hover:border-amber-200 transition-all duration-200 hover:shadow-md"
          >
            <div class="flex items-start justify-between mb-4">
              <div>
                <h3 class="text-lg font-semibold text-gray-800">{{ device.deviceName }}</h3>
                <p class="text-sm text-gray-500">{{ device.deviceCode }}</p>
              </div>
              <span
                :class="[
                  'badge',
                  device.status === 'ONLINE' ? 'badge-success' : 'badge-warning'
                ]"
              >
                {{ device.status === 'ONLINE' ? '在线' : '离线' }}
              </span>
            </div>

            <div class="space-y-2 text-sm">
              <div v-if="device.location" class="flex items-center text-gray-600">
                <i class="fas fa-map-marker-alt w-5 text-amber-500"></i>
                <span>{{ device.location }}</span>
              </div>
              <div v-if="device.description" class="flex items-center text-gray-600">
                <i class="fas fa-info-circle w-5 text-amber-500"></i>
                <span class="truncate">{{ device.description }}</span>
              </div>
              <div class="flex items-center text-gray-600">
                <i class="fas fa-clock w-5 text-amber-500"></i>
                <span>最后心跳: {{ formatDate(device.lastHeartbeat) }}</span>
              </div>
              <div v-if="device.currentContent" class="mt-3 p-3 bg-amber-50 rounded-lg">
                <p class="text-xs text-amber-700 font-medium mb-1">正在播放</p>
                <p class="text-sm text-gray-700 truncate">{{ device.currentContent.fileName }}</p>
              </div>
            </div>

            <div class="flex items-center gap-2 mt-4 pt-4 border-t border-gray-100">
              <button @click="viewDevice(device)" class="flex-1 btn-secondary text-sm py-2">
                <i class="fas fa-eye mr-1"></i>
                详情
              </button>
              <button
                @click="editDevice(device)"
                class="flex-1 btn-secondary text-sm py-2 text-amber-600 hover:bg-amber-50"
              >
                <i class="fas fa-edit mr-1"></i>
                编辑
              </button>
              <button
                @click="confirmDelete(device)"
                class="flex-1 btn-secondary text-sm py-2 text-red-500 hover:bg-red-50"
              >
                <i class="fas fa-trash mr-1"></i>
                删除
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingDevice ? '编辑设备' : '添加设备'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="form.deviceName" placeholder="请输入设备名称" class="input-field" />
        </el-form-item>
        <el-form-item label="设备编码" prop="deviceCode">
          <el-input
            v-model="form.deviceCode"
            placeholder="请输入设备编码"
            :disabled="!!editingDevice"
            class="input-field"
          />
        </el-form-item>
        <el-form-item label="MQTT主题">
          <el-input v-model="form.mqttTopic" placeholder="不填则自动生成" class="input-field" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" placeholder="请输入位置" class="input-field" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
            class="input-field"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <button @click="dialogVisible = false" class="btn-secondary">取消</button>
        <button @click="handleSubmit" :disabled="submitting" class="btn-primary ml-3">
          <el-icon v-if="submitting" class="is-loading mr-1"><Loading /></el-icon>
          {{ editingDevice ? '保存' : '添加' }}
        </button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="设备详情" width="600px">
      <div v-if="selectedDevice" class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <p class="text-xs text-gray-500 mb-1">设备名称</p>
            <p class="text-gray-800 font-medium">{{ selectedDevice.deviceName }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 mb-1">设备编码</p>
            <p class="text-gray-800 font-medium">{{ selectedDevice.deviceCode }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500 mb-1">状态</p>
            <span
              :class="[
                'badge',
                selectedDevice.status === 'ONLINE' ? 'badge-success' : 'badge-warning'
              ]"
            >
              {{ selectedDevice.status === 'ONLINE' ? '在线' : '离线' }}
            </span>
          </div>
          <div>
            <p class="text-xs text-gray-500 mb-1">MQTT主题</p>
            <p class="text-gray-800 font-mono text-sm">{{ selectedDevice.mqttTopic }}</p>
          </div>
        </div>
        <div v-if="selectedDevice.location">
          <p class="text-xs text-gray-500 mb-1">位置</p>
          <p class="text-gray-800">{{ selectedDevice.location }}</p>
        </div>
        <div v-if="selectedDevice.description">
          <p class="text-xs text-gray-500 mb-1">描述</p>
          <p class="text-gray-800">{{ selectedDevice.description }}</p>
        </div>
        <div>
          <p class="text-xs text-gray-500 mb-1">最后心跳</p>
          <p class="text-gray-800">{{ formatDate(selectedDevice.lastHeartbeat) }}</p>
        </div>
        <div>
          <p class="text-xs text-gray-500 mb-1">创建时间</p>
          <p class="text-gray-800">{{ formatDate(selectedDevice.createTime) }}</p>
        </div>
        <div v-if="selectedDevice.currentContent" class="p-4 bg-amber-50 rounded-xl">
          <p class="text-xs text-amber-700 font-medium mb-2">当前播放内容</p>
          <p class="text-gray-800">{{ selectedDevice.currentContent.fileName }}</p>
          <p class="text-sm text-gray-500 mt-1">
            {{ formatFileSize(selectedDevice.currentContent.fileSize) }}
          </p>
        </div>
      </div>
      <template #footer>
        <button @click="detailVisible = false" class="btn-secondary">关闭</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { deviceApi, adminApi } from '@/api'
import type { DeviceVO, DeviceDTO } from '@/types'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const devices = ref<DeviceVO[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingDevice = ref<DeviceVO | null>(null)
const selectedDevice = ref<DeviceVO | null>(null)
const formRef = ref<FormInstance>()

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
    type: 'warning',
    confirmButtonClass: 'el-button--danger'
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

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadDevices()
})
</script>