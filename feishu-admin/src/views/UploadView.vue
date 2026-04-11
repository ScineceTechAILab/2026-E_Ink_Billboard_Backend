<template>
  <div class="min-h-screen p-6">
    <div class="max-w-6xl mx-auto space-y-6">
      <div class="card animate-slide-up">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-800 mb-1">
              <span class="mr-2">📸</span>
              图片上传
            </h1>
            <p class="text-gray-500">支持拖拽、点击、粘贴三种上传方式 ✨</p>
          </div>
          <button @click="goBack" class="btn-secondary">
            <span class="mr-2">←</span>
            返回
          </button>
        </div>

        <!-- 分页 -->
        <div class="flex justify-center mt-6" v-if="pagination.total > 0">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>

      <div class="card animate-slide-up" style="animation-delay: 0.1s">
        <div
          ref="dropZoneRef"
          class="drop-zone border-3 border-dashed rounded-2xl p-12 text-center cursor-pointer transition-all duration-300"
          :class="
            isDragOver
              ? 'border-amber-400 bg-amber-50'
              : 'border-gray-200 hover:border-amber-300 hover:bg-amber-50/50'
          "
          @dragover.prevent="isDragOver = true"
          @dragleave.prevent="isDragOver = false"
          @drop.prevent="handleDrop"
          @click="triggerFileInput"
        >
          <input
            ref="fileInputRef"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            multiple
            @change="handleFileSelect"
            class="hidden"
          />
          <div class="text-6xl mb-4">
            {{ isDragOver ? '📥' : '🖼️' }}
          </div>
          <p class="text-lg font-semibold text-gray-700 mb-2">
            {{ isDragOver ? '松开鼠标上传图片' : '拖拽图片到这里，或点击选择' }}
          </p>
          <p class="text-gray-500 text-sm">
            支持 JPG、PNG、GIF、WebP 格式，单张 ≤ 5MB，最多 20 张
          </p>
        </div>
      </div>

      <div v-if="uploadingFiles.length > 0" class="space-y-6">
        <div class="card animate-slide-up" style="animation-delay: 0.2s">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold text-gray-800">
              <span class="mr-2">📤</span>
              上传队列
            </h2>
            <div class="text-sm text-gray-500">总体进度: {{ overallProgress }}%</div>
          </div>
          <el-progress :percentage="overallProgress" :stroke-width="8" :color="progressColor" />
        </div>

        <div class="card animate-slide-up" style="animation-delay: 0.3s">
          <div class="space-y-4">
            <div
              v-for="file in uploadingFiles"
              :key="file.id"
              class="flex items-center gap-4 p-4 bg-gray-50 rounded-xl"
            >
              <div class="w-16 h-16 bg-gray-200 rounded-lg overflow-hidden flex-shrink-0">
                <img v-if="file.url" :src="file.url" class="w-full h-full object-cover" />
                <div v-else class="w-full h-full flex items-center justify-center text-2xl">
                  🖼️
                </div>
              </div>

              <div class="flex-1 min-w-0">
                <p class="font-medium text-gray-800 truncate">{{ file.file.name }}</p>
                <p class="text-sm text-gray-500">
                  {{ formatFileSize(file.originalSize) }}
                  <span v-if="file.compressedSize" class="ml-2 text-green-600">
                    → {{ formatFileSize(file.compressedSize) }} (节省
                    {{ formatFileSize(file.originalSize - file.compressedSize) }})
                  </span>
                </p>
                <div class="mt-2">
                  <div v-if="file.status === 'uploading'" class="flex items-center gap-2">
                    <el-progress :percentage="file.progress" :stroke-width="6" style="flex: 1" />
                    <button
                      @click="cancelUpload(file)"
                      class="text-gray-400 hover:text-red-500"
                      aria-label="取消上传"
                    >
                      <el-icon><Close /></el-icon>
                    </button>
                  </div>
                  <div v-else-if="file.status === 'success'" class="flex items-center gap-2 text-green-600">
                    <el-icon><Check /></el-icon>
                    <span>上传成功</span>
                  </div>
                  <div v-else-if="file.status === 'error'" class="flex items-center gap-2">
                    <span class="text-red-500">{{ file.error }}</span>
                    <button
                      @click="retryUpload(file)"
                      class="text-amber-500 hover:text-amber-600"
                      aria-label="重新上传"
                    >
                      <el-icon><Refresh /></el-icon>
                    </button>
                  </div>
                  <div v-else class="text-gray-400">等待上传...</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="card animate-slide-up" style="animation-delay: 0.4s">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-semibold text-gray-800">
            <span class="mr-2">✅</span>
            已上传图片
          </h2>
        </div>

        <!-- 查询区块 -->
        <div class="mb-6 p-4 bg-gray-50 rounded-xl space-y-4">
          <div class="flex flex-wrap items-center gap-4">
            <!-- 搜索框 -->
            <div class="flex-1 min-w-[200px]">
              <el-input
                v-model="searchForm.fileName"
                placeholder="搜索图片名称..."
                clearable
                :prefix-icon="Search"
                @input="handleSearch"
              />
            </div>
            
            <!-- 用户筛选 -->
            <div class="w-[200px]">
              <el-select
                v-model="searchForm.userIds"
                multiple
                collapse-tags
                collapse-tags-tooltip
                placeholder="筛选上传用户"
                clearable
                filterable
                remote
                :remote-method="remoteUserMethod"
                :loading="userLoading"
                @change="handleSearch"
                no-match-text="无相关用户"
                no-data-text="无相关用户"
              >
                <el-option
                  v-for="user in filteredUserList"
                  :key="user.id"
                  :label="user.nickname"
                  :value="user.id"
                />
              </el-select>
            </div>

            <!-- 排序 -->
            <el-button-group>
              <el-button 
                :type="searchForm.sortOrder === 'desc' ? 'primary' : 'default'" 
                @click="toggleSort('desc')"
              >
                时间倒序
              </el-button>
              <el-button 
                :type="searchForm.sortOrder === 'asc' ? 'primary' : 'default'" 
                @click="toggleSort('asc')"
              >
                时间正序
              </el-button>
            </el-button-group>

            <!-- 重置 -->
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </div>
        </div>

        <div v-if="uploadedImages.length > 0" class="columns-1 sm:columns-2 md:columns-3 lg:columns-4 gap-4 space-y-4">
          <div
            v-for="image in uploadedImages"
            :key="image.id"
            class="break-inside-avoid bg-white rounded-xl overflow-hidden shadow-sm hover:shadow-md transition-shadow"
          >
            <div class="relative group">
              <img :src="image.processedUrl || image.originalUrl" class="w-full" alt="上传的图片" />
              <div
                class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2"
              >
                <button
                  @click="viewImage(image.processedUrl || image.originalUrl)"
                  class="w-10 h-10 bg-white rounded-full flex items-center justify-center text-gray-700 hover:bg-amber-100 transition-colors"
                  aria-label="查看原图"
                >
                  <el-icon><View /></el-icon>
                </button>
                <button
                  @click="copyImageUrl(image.processedUrl || image.originalUrl)"
                  class="w-10 h-10 bg-white rounded-full flex items-center justify-center text-gray-700 hover:bg-amber-100 transition-colors"
                  aria-label="复制链接"
                >
                  <el-icon><DocumentCopy /></el-icon>
                </button>
                <button
                  @click="deleteImage(image)"
                  class="w-10 h-10 bg-white rounded-full flex items-center justify-center text-red-500 hover:bg-red-100 transition-colors"
                  aria-label="删除图片"
                >
                  <el-icon><Delete /></el-icon>
                </button>
              </div>
            </div>
            <div class="p-3">
              <p class="text-sm text-gray-700 truncate">{{ image.fileName }}</p>
              <div class="flex items-center justify-between mt-2">
                <span
                  :class="[
                    'badge',
                    image.auditStatus === 'APPROVED' ? 'badge-success' : 'badge-warning'
                  ]"
                >
                  {{ image.auditStatus === 'APPROVED' ? '已通过' : '审核中' }}
                </span>
                <span class="text-xs text-gray-400">{{ formatFileSize(image.fileSize) }}</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无图片" />
      </div>

      <el-image-viewer
        v-if="previewVisible"
        :url-list="previewUrlList"
        :initial-index="previewIndex"
        @close="previewVisible = false"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Check, Refresh, View, DocumentCopy, Delete, Search } from '@element-plus/icons-vue'
import imageCompression from 'browser-image-compression'
import { imageApi, adminApi } from '@/api'
import type { UploadingFile, ImageVO, UserVO } from '@/types'

const router = useRouter()
const dropZoneRef = ref<HTMLDivElement>()
const fileInputRef = ref<HTMLInputElement>()
const isDragOver = ref(false)
const uploadingFiles = ref<UploadingFile[]>([])
const uploadedImages = ref<ImageVO[]>([])
const previewVisible = ref(false)
const previewUrlList = ref<string[]>([])
const previewIndex = ref(0)
const cancelControllers = ref<Map<string, AbortController>>(new Map())
const retryCounts = ref<Map<string, number>>(new Map())

// 查询相关
const searchForm = ref({
  fileName: '',
  userIds: [] as number[],
  sortOrder: 'desc' as 'asc' | 'desc'
})
const userList = ref<UserVO[]>([])
const filteredUserList = ref<UserVO[]>([])
const userLoading = ref(false)
const pagination = ref({
  current: 1,
  size: 20,
  total: 0
})
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
const MAX_FILE_SIZE = 5 * 1024 * 1024
const MAX_FILES = 20
const MAX_RETRIES = 3
const COMPRESS_OPTIONS = {
  maxWidthOrHeight: 1920,
  maxSizeMB: 5,
  useWebWorker: true,
  fileType: 'image/jpeg',
  initialQuality: 0.8
}

const overallProgress = computed(() => {
  if (uploadingFiles.value.length === 0) return 0
  const total = uploadingFiles.value.reduce((sum, f) => sum + f.progress, 0)
  return Math.round(total / uploadingFiles.value.length)
})

const progressColor = computed(() => {
  if (overallProgress.value === 100) return '#52c41a'
  if (overallProgress.value > 50) return '#faad14'
  return '#f97316'
})

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

const generateId = (): string => {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

const validateFile = (file: File): boolean => {
  if (!ALLOWED_TYPES.includes(file.type)) {
    ElMessage.error(`不支持的文件类型: ${file.name}`)
    return false
  }
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.error(`文件过大: ${file.name} (最大 5MB)`)
    return false
  }
  return true
}

const compressImage = async (file: File): Promise<File> => {
  try {
    const compressedFile = await imageCompression(file, COMPRESS_OPTIONS)
    return new File([compressedFile], file.name, { type: compressedFile.type })
  } catch (error) {
    console.warn('Compression failed, using original file:', error)
    return file
  }
}

const handleFiles = async (files: FileList | File[]) => {
  const fileArray = Array.from(files)

  if (fileArray.length + uploadingFiles.value.length > MAX_FILES) {
    ElMessage.error(`最多只能上传 ${MAX_FILES} 张图片`)
    return
  }

  const validFiles = fileArray.filter(validateFile)

  for (const file of validFiles) {
    const id = generateId()
    const compressedFile = await compressImage(file)

    const uploadingFile: UploadingFile = {
      id,
      file: compressedFile,
      progress: 0,
      status: 'pending',
      originalSize: file.size,
      compressedSize: compressedFile.size
    }

    uploadingFiles.value.push(uploadingFile)
    retryCounts.value.set(id, 0)

    await uploadFile(uploadingFile)
  }
}

const uploadFile = async (uploadingFile: UploadingFile) => {
  const fileIndex = uploadingFiles.value.findIndex((f) => f.id === uploadingFile.id)
  if (fileIndex === -1) return

  uploadingFiles.value[fileIndex].status = 'uploading'

  try {
    const uploadId = uploadingFile.id
    const res = await imageApi.upload(
      uploadingFile.file,
      (progressEvent: any) => {
        if (progressEvent.total > 0) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          const idx = uploadingFiles.value.findIndex((f) => f.id === uploadingFile.id)
          if (idx !== -1) {
            uploadingFiles.value[idx].progress = progress
          }
        }
      },
      uploadId
    )

    const idx = uploadingFiles.value.findIndex((f) => f.id === uploadingFile.id)
    if (idx !== -1) {
      uploadingFiles.value[idx].status = 'success'
      uploadingFiles.value[idx].url = res.data.url
      uploadingFiles.value[idx].progress = 100

      const imageVO: ImageVO = {
        id: res.data.id,
        userId: 0,
        fileName: uploadingFile.file.name,
        fileSize: uploadingFile.compressedSize || uploadingFile.file.size,
        originalUrl: res.data.url,
        processedUrl: res.data.url,
        auditStatus: res.data.auditStatus,
        auditReason: null,
        createTime: new Date().toISOString()
      }
      // uploadedImages.value.unshift(imageVO)
      // 重新加载列表以保持一致性
      handleSearch()

      ElMessage.success(res.data.auditMessage || '上传成功')
    }
  } catch (error: any) {
    const idx = uploadingFiles.value.findIndex((f) => f.id === uploadingFile.id)
    if (idx !== -1) {
      const currentRetry = retryCounts.value.get(uploadingFile.id) || 0

      if (currentRetry < MAX_RETRIES) {
        retryCounts.value.set(uploadingFile.id, currentRetry + 1)
        ElMessage.warning(`上传失败，正在重试 (${currentRetry + 1}/${MAX_RETRIES})...`)
        await new Promise((resolve) => setTimeout(resolve, 1000))
        await uploadFile(uploadingFile)
      } else {
        uploadingFiles.value[idx].status = 'error'
        uploadingFiles.value[idx].error = error.message || '上传失败'
      }
    }
  }
}

const cancelUpload = (file: UploadingFile) => {
  const controller = cancelControllers.value.get(file.id)
  if (controller) {
    controller.abort()
    cancelControllers.value.delete(file.id)
  }
  const index = uploadingFiles.value.findIndex((f) => f.id === file.id)
  if (index !== -1) {
    uploadingFiles.value.splice(index, 1)
  }
  ElMessage.info('已取消上传')
}

const retryUpload = async (file: UploadingFile) => {
  const index = uploadingFiles.value.findIndex((f) => f.id === file.id)
  if (index !== -1) {
    uploadingFiles.value[index].status = 'pending'
    uploadingFiles.value[index].progress = 0
    uploadingFiles.value[index].error = undefined
    retryCounts.value.set(file.id, 0)
    await uploadFile(file)
  }
}

const handleDrop = (e: DragEvent) => {
  isDragOver.value = false
  if (e.dataTransfer?.files) {
    handleFiles(e.dataTransfer.files)
  }
}

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files) {
    handleFiles(target.files)
    target.value = ''
  }
}

const handlePaste = (e: ClipboardEvent) => {
  const items = e.clipboardData?.items
  if (items) {
    const files: File[] = []
    for (const item of items) {
      if (item.type.indexOf('image') !== -1) {
        const file = item.getAsFile()
        if (file) files.push(file)
      }
    }
    if (files.length > 0) {
      e.preventDefault()
      handleFiles(files)
    }
  }
}

const viewImage = (url: string) => {
  previewUrlList.value = [url]
  previewIndex.value = 0
  previewVisible.value = true
}

const copyImageUrl = async (url: string) => {
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('链接已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败，请手动复制')
  }
}

const deleteImage = async (image: ImageVO) => {
  try {
    await ElMessageBox.confirm('确定要删除这张图片吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await imageApi.delete(image.id)
    uploadedImages.value = uploadedImages.value.filter((img) => img.id !== image.id)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
    }
  }
}

const goBack = () => {
  router.back()
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

const handleSearch = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    pagination.value.current = 1
    loadUploadedImages()
  }, 300)
}

const toggleSort = (order: 'asc' | 'desc') => {
  if (searchForm.value.sortOrder === order) return
  searchForm.value.sortOrder = order
  handleSearch() // 立即触发，不需要防抖
}

const handleReset = () => {
  searchForm.value = {
    fileName: '',
    userIds: [],
    sortOrder: 'desc'
  }
  handleSearch()
}

const handlePageChange = () => {
  loadUploadedImages()
}

const loadUploadedImages = async () => {
  try {
    const res = await imageApi.list({
      auditStatus: 'APPROVED',
      size: pagination.value.size,
      current: pagination.value.current,
      fileName: searchForm.value.fileName,
      userIds: searchForm.value.userIds,
      sortOrder: searchForm.value.sortOrder
    })
    if (res.code === 200) {
      uploadedImages.value = res.data.records
      pagination.value.total = res.data.total
    }
  } catch (error) {
    console.error('Load images failed:', error)
  }
}

onMounted(() => {
  document.addEventListener('paste', handlePaste)
  loadUploadedImages()
  loadUsers()
})

onUnmounted(() => {
  document.removeEventListener('paste', handlePaste)
})
</script>

<style scoped>
.drop-zone {
  min-height: 200px;
}
</style>