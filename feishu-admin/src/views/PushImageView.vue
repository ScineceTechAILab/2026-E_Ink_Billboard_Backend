<template>
  <div class="min-h-screen p-6">
    <div class="max-w-7xl mx-auto space-y-6">
      <div class="card animate-slide-up">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-800 mb-1">
              <i class="fas fa-paper-plane mr-2 text-amber-500"></i>
              图片推送
            </h1>
            <p class="text-gray-500">选择图片并推送到设备</p>
          </div>
          <button @click="goBack" class="btn-secondary">
            <i class="fas fa-arrow-left mr-2"></i>
            返回
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="card animate-slide-up" style="animation-delay: 0.1s">
          <h2 class="text-lg font-semibold text-gray-800 mb-4">
            <i class="fas fa-images mr-2 text-amber-500"></i>
            选择图片
          </h2>
          
          <div v-if="imagesLoading" class="flex items-center justify-center py-12">
            <el-icon class="is-loading text-4xl text-amber-500"><Loading /></el-icon>
            <span class="ml-3 text-gray-500">加载图片列表...</span>
          </div>

          <div v-else class="space-y-4">
            <div class="flex items-center gap-3">
              <el-select
                v-model="imageAuditFilter"
                placeholder="审核状态"
                clearable
                style="width: 150px"
                @change="loadImages"
              >
                <el-option label="全部" value="" />
                <el-option label="待审核" value="PENDING" />
                <el-option label="已通过" value="APPROVED" />
              </el-select>
              <input
                v-model="imageSearch"
                type="text"
                placeholder="搜索文件名..."
                class="input-field flex-1"
              />
            </div>

            <div class="grid grid-cols-2 gap-3 max-h-96 overflow-y-auto">
              <div
                v-for="image in filteredImages"
                :key="image.id"
                @click="selectImage(image)"
                :class="[
                  'p-3 rounded-lg border-2 cursor-pointer transition-all',
                  selectedImage?.id === image.id
                    ? 'border-amber-500 bg-amber-50'
                    : 'border-gray-100 hover:border-amber-200'
                ]"
              >
                <div class="aspect-square bg-gray-100 rounded-lg mb-2 overflow-hidden">
                  <img
                    :src="image.processedUrl || image.originalUrl"
                    :alt="image.fileName"
                    class="w-full h-full object-cover"
                  />
                </div>
                <p class="text-sm text-gray-700 truncate">{{ image.fileName }}</p>
                <p class="text-xs text-gray-500">{{ formatFileSize(image.fileSize) }}</p>
                <span
                  :class="[
                    'badge text-xs mt-1',
                    image.auditStatus === 'APPROVED'
                      ? 'badge-success'
                      : 'badge-warning'
                  ]"
                >
                  {{ image.auditStatus === 'APPROVED' ? '已通过' : '待审核' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="card animate-slide-up" style="animation-delay: 0.2s">
          <h2 class="text-lg font-semibold text-gray-800 mb-4">
            <i class="fas fa-tv mr-2 text-amber-500"></i>
            选择设备
          </h2>

          <div v-if="devicesLoading" class="flex items-center justify-center py-12">
            <el-icon class="is-loading text-4xl text-amber-500"><Loading /></el-icon>
            <span class="ml-3 text-gray-500">加载设备列表...</span>
          </div>

          <div v-else class="space-y-4">
            <div class="flex items-center gap-3">
              <el-select
                v-model="deviceStatusFilter"
                placeholder="设备状态"
                clearable
                style="width: 150px"
                @change="loadDevices"
              >
                <el-option label="全部" value="" />
                <el-option label="在线" value="ONLINE" />
                <el-option label="离线" value="OFFLINE" />
              </el-select>
              <input
                v-model="deviceSearch"
                type="text"
                placeholder="搜索设备名称..."
                class="input-field flex-1"
              />
            </div>

            <div class="space-y-2 max-h-80 overflow-y-auto">
              <div
                v-for="device in filteredDevices"
                :key="device.id"
                @click="toggleDevice(device)"
                :class="[
                  'p-4 rounded-lg border-2 cursor-pointer transition-all',
                  selectedDeviceIds.includes(device.id)
                    ? 'border-amber-500 bg-amber-50'
                    : 'border-gray-100 hover:border-amber-200'
                ]"
              >
                <div class="flex items-center justify-between">
                  <div>
                    <div class="flex items-center gap-2">
                      <h3 class="font-medium text-gray-800">{{ device.deviceName }}</h3>
                      <span
                        :class="[
                          'badge text-xs',
                          device.status === 'ONLINE' ? 'badge-success' : 'badge-warning'
                        ]"
                      >
                        {{ device.status === 'ONLINE' ? '在线' : '离线' }}
                      </span>
                    </div>
                    <p class="text-sm text-gray-500">{{ device.deviceCode }}</p>
                    <p v-if="device.location" class="text-xs text-gray-400">
                      <i class="fas fa-map-marker-alt mr-1"></i>
                      {{ device.location }}
                    </p>
                  </div>
                  <div v-if="selectedDeviceIds.includes(device.id)">
                    <i class="fas fa-check-circle text-amber-500 text-xl"></i>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="card animate-slide-up" style="animation-delay: 0.3s">
        <h2 class="text-lg font-semibold text-gray-800 mb-4">
          <i class="fas fa-info-circle mr-2 text-amber-500"></i>
          推送预览
        </h2>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h3 class="text-sm font-medium text-gray-600 mb-2">选中图片</h3>
            <div v-if="selectedImage" class="flex items-center gap-4 p-4 bg-gray-50 rounded-lg">
              <div class="w-20 h-20 bg-gray-200 rounded-lg overflow-hidden">
                <img
                  :src="selectedImage.processedUrl || selectedImage.originalUrl"
                  :alt="selectedImage.fileName"
                  class="w-full h-full object-cover"
                />
              </div>
              <div>
                <p class="font-medium text-gray-800">{{ selectedImage.fileName }}</p>
                <p class="text-sm text-gray-500">{{ formatFileSize(selectedImage.fileSize) }}</p>
              </div>
            </div>
            <div v-else class="p-4 bg-gray-50 rounded-lg text-gray-400">
              请选择图片
            </div>
          </div>

          <div>
            <h3 class="text-sm font-medium text-gray-600 mb-2">选中设备 ({{ selectedDeviceIds.length }})</h3>
            <div v-if="selectedDeviceIds.length > 0" class="flex flex-wrap gap-2">
              <span
                v-for="deviceId in selectedDeviceIds"
                :key="deviceId"
                class="px-3 py-1 bg-amber-100 text-amber-700 rounded-full text-sm"
              >
                {{ getDeviceById(deviceId)?.deviceName }}
              </span>
            </div>
            <div v-else class="p-4 bg-gray-50 rounded-lg text-gray-400">
              请选择设备
            </div>
          </div>
        </div>

        <div class="flex items-center justify-end gap-4 mt-6 pt-4 border-t border-gray-100">
          <button @click="resetSelection" class="btn-secondary">
            <i class="fas fa-redo mr-2"></i>
            重置
          </button>
          <button
            @click="handlePush"
            :disabled="!canPush || pushing"
            class="btn-primary"
          >
            <el-icon v-if="pushing" class="is-loading mr-2"><Loading /></el-icon>
            <i v-else class="fas fa-paper-plane mr-2"></i>
            {{ pushing ? '推送中...' : '推送图片' }}
          </button>
        </div>
      </div>

      <div v-if="pushResults.length > 0" class="card animate-slide-up" style="animation-delay: 0.4s">
        <h2 class="text-lg font-semibold text-gray-800 mb-4">
          <i class="fas fa-list-alt mr-2 text-amber-500"></i>
          推送结果
        </h2>
        <div class="space-y-2">
          <div
            v-for="result in pushResults"
            :key="result.deviceId"
            :class="[
              'p-4 rounded-lg border-l-4',
              result.success ? 'border-green-500 bg-green-50' : 'border-red-500 bg-red-50'
            ]"
          >
            <div class="flex items-center justify-between">
              <div>
                <div class="flex items-center gap-2">
                  <i
                    :class="[
                      'text-xl',
                      result.success ? 'fas fa-check-circle text-green-500' : 'fas fa-times-circle text-red-500'
                    ]"
                  ></i>
                  <span class="font-medium text-gray-800">{{ result.deviceName }}</span>
                </div>
                <p v-if="result.error" class="text-sm text-red-600 mt-1">
                  {{ result.error }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { imageApi, deviceApi, pushApi } from '@/api'
import type { ImageVO, DeviceVO } from '@/types'

const router = useRouter()

const imagesLoading = ref(false)
const devicesLoading = ref(false)
const pushing = ref(false)

const images = ref<ImageVO[]>([])
const devices = ref<DeviceVO[]>([])

const imageSearch = ref('')
const imageAuditFilter = ref('')
const deviceSearch = ref('')
const deviceStatusFilter = ref('')

const selectedImage = ref<ImageVO | null>(null)
const selectedDeviceIds = ref<number[]>([])
const pushResults = ref<Array<{ deviceId: number; deviceName: string; success: boolean; error?: string }>>([])

const filteredImages = computed(() => {
  return images.value.filter((image) => {
    const matchesSearch =
      !imageSearch.value || image.fileName.toLowerCase().includes(imageSearch.value.toLowerCase())
    const matchesStatus = !imageAuditFilter.value || image.auditStatus === imageAuditFilter.value
    return matchesSearch && matchesStatus
  })
})

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

const loadImages = async () => {
  imagesLoading.value = true
  try {
    const res = await imageApi.list({ auditStatus: imageAuditFilter.value || undefined })
    if (res.code === 200) {
      images.value = res.data.records
    }
  } catch (error) {
    console.error('Load images failed:', error)
    ElMessage.error('加载图片列表失败')
  } finally {
    imagesLoading.value = false
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
  loadImages()
  loadDevices()
})
</script>
