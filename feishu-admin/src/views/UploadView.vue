<template>
  <div class="ink-upload-page">
    <!-- Background -->
    <div class="ink-grid-bg" />

    <!-- Header -->
    <header class="ink-page-header">
      <div class="ink-container">
        <div class="ink-header-content">
          <div class="ink-header-brand">
            <div class="ink-header-icon ink-glow-box">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12" />
              </svg>
            </div>
            <div>
              <h1 class="ink-heading ink-heading-4">图片上传</h1>
              <p class="ink-body">支持拖拽、点击、粘贴三种上传方式</p>
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
        <!-- Upload Zone -->
        <div
          class="ink-card ink-upload-zone"
          :class="{ 'ink-upload-active': isDragOver, 'ink-upload-disabled': uploadingFiles.length > 0 }"
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
            class="ink-hidden-input"
          />
          <div class="ink-upload-content">
            <div class="ink-upload-icon" :class="{ 'ink-upload-pulse': isDragOver }">
              <svg v-if="isDragOver" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
              </svg>
            </div>
            <h3 class="ink-upload-title">
              {{ isDragOver ? '松开鼠标上传图片' : '拖拽图片到这里' }}
            </h3>
            <p class="ink-upload-hint">
              或点击选择文件 · 支持 JPG、PNG、GIF、WebP · 单张 ≤ 5MB
            </p>
            <div class="ink-upload-formats">
              <span class="ink-format-tag">JPG</span>
              <span class="ink-format-tag">PNG</span>
              <span class="ink-format-tag">GIF</span>
              <span class="ink-format-tag">WebP</span>
            </div>
          </div>
        </div>

        <!-- Upload Progress -->
        <div v-if="uploadingFiles.length > 0" class="ink-upload-progress ink-card">
          <div class="ink-progress-header">
            <h3 class="ink-card-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
              </svg>
              上传队列 ({{ uploadingFiles.length }})
            </h3>
            <span class="ink-progress-text">{{ overallProgress }}%</span>
          </div>
          <div class="ink-progress">
            <div
              class="ink-progress-bar"
              :style="{ width: overallProgress + '%' }"
            />
          </div>
        </div>

        <!-- Uploading Files List -->
        <div v-if="uploadingFiles.length > 0" class="ink-files-list ink-card">
          <div
            v-for="file in uploadingFiles"
            :key="file.id"
            class="ink-file-item"
          >
            <div class="ink-file-preview">
              <img v-if="file.url" :src="file.url" :alt="file.file.name" />
              <div v-else class="ink-file-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <polyline points="21 15 16 10 5 21" />
                </svg>
              </div>
            </div>
            <div class="ink-file-info">
              <p class="ink-file-name">{{ file.file.name }}</p>
              <p class="ink-file-size">
                {{ formatFileSize(file.originalSize) }}
                <span v-if="file.compressedSize" class="ink-file-saved">
                  → {{ formatFileSize(file.compressedSize) }}
                  (节省 {{ formatFileSize(file.originalSize - file.compressedSize) }})
                </span>
              </p>
              <div class="ink-file-progress">
                <div v-if="file.status === 'uploading'" class="ink-file-progress-bar">
                  <div
                    class="ink-progress ink-progress-sm"
                    style="flex: 1;"
                  >
                    <div
                      class="ink-progress-bar"
                      :style="{ width: file.progress + '%' }"
                    />
                  </div>
                  <button @click.stop="cancelUpload(file)" class="ink-icon-btn ink-icon-btn-sm">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <line x1="18" y1="6" x2="6" y2="18" />
                      <line x1="6" y1="6" x2="18" y2="18" />
                    </svg>
                  </button>
                </div>
                <div v-else-if="file.status === 'success'" class="ink-file-status ink-file-status-success">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                  </svg>
                  上传成功
                </div>
                <div v-else-if="file.status === 'error'" class="ink-file-status ink-file-status-error">
                  <span>{{ file.error }}</span>
                  <button @click.stop="retryUpload(file)" class="ink-btn ink-btn-ghost ink-btn-xs">
                    重试
                  </button>
                </div>
                <div v-else class="ink-file-status ink-file-status-pending">等待中...</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Search & Filter -->
        <div class="ink-card ink-filter-bar">
          <div class="ink-filter-group">
            <div class="ink-search-box">
              <svg class="ink-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8" />
                <path d="M21 21l-4.35-4.35" />
              </svg>
              <input
                v-model="searchForm.fileName"
                type="text"
                placeholder="搜索图片名称..."
                class="ink-input"
                @input="handleSearch"
              />
            </div>

            <el-select
              v-model="searchForm.userIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="筛选用户"
              clearable
              filterable
              remote
              :remote-method="remoteUserMethod"
              :loading="userLoading"
              @change="handleSearch"
              class="ink-select"
            >
              <el-option
                v-for="user in filteredUserList"
                :key="user.id"
                :label="user.nickname"
                :value="user.id"
              />
            </el-select>

            <div class="ink-sort-toggle">
              <button
                :class="['ink-sort-btn', { 'ink-sort-active': searchForm.sortOrder === 'desc' }]"
                @click="toggleSort('desc')"
              >
                最新
              </button>
              <button
                :class="['ink-sort-btn', { 'ink-sort-active': searchForm.sortOrder === 'asc' }]"
                @click="toggleSort('asc')"
              >
                最早
              </button>
            </div>

            <button @click="handleReset" class="ink-btn ink-btn-ghost">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 4 23 10 17 10" />
                <path d="M20.49 15a9 9 0 11-2.12-9.36L23 10" />
              </svg>
              重置
            </button>
          </div>
        </div>

        <!-- Images Gallery -->
        <div class="ink-images-section ink-card">
          <h3 class="ink-card-title">
<!--            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">-->
<!--              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />-->
<!--              <circle cx="8.5" cy="8.5" r="1.5" />-->
<!--              <polyline points="21 15 16 10 5 21" />-->
<!--            </svg>-->
            已上传图片 ({{ pagination.total }})
          </h3>

          <div v-if="uploadedImages.length > 0" class="ink-images-grid">
            <div
              v-for="image in uploadedImages"
              :key="image.id"
              class="ink-image-card"
            >
              <div class="ink-image-wrapper">
                <img
                  :src="image.processedUrl || image.originalUrl"
                  :alt="image.fileName"
                  loading="lazy"
                  @click="viewImage(image.processedUrl || image.originalUrl)"
                />
                <div class="ink-image-overlay">
                  <button @click.stop="viewImage(image.processedUrl || image.originalUrl)" class="ink-overlay-btn">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  </button>
                  <button @click.stop="copyImageUrl(image.processedUrl || image.originalUrl)" class="ink-overlay-btn">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                      <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1" />
                    </svg>
                  </button>
                  <button @click.stop="deleteImage(image)" class="ink-overlay-btn ink-overlay-btn-danger">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6" />
                      <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
                    </svg>
                  </button>
                </div>
              </div>
              <div class="ink-image-meta">
                <p class="ink-image-name" :title="image.fileName">{{ image.fileName }}</p>
                <div class="ink-image-footer">
                  <span
                    :class="[
                      'ink-badge',
                      image.auditStatus === 'APPROVED' ? 'ink-badge-success' : 'ink-badge-warning'
                    ]"
                  >
                    {{ image.auditStatus === 'APPROVED' ? '已通过' : '审核中' }}
                  </span>
                  <span class="ink-image-size">{{ formatFileSize(image.fileSize) }}</span>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="ink-empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
            <p>暂无图片</p>
            <p class="ink-empty-hint">点击上方区域或拖拽图片开始上传</p>
          </div>

          <!-- Pagination -->
          <div v-if="pagination.total > pagination.size" class="ink-pagination">
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
      </div>
    </main>

    <!-- Image Viewer -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewUrlList"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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

// Search & Filter
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
  handleSearch()
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
@import '../styles/design-system.css';

.ink-upload-page {
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

.ink-hidden-input {
  display: none;
}

/* Upload Zone */
.ink-upload-zone {
  padding: var(--space-6);
  text-align: center;
  cursor: pointer;
  border: 2px dashed var(--ghost-border);
  background: linear-gradient(145deg, rgba(28, 28, 31, 0.5) 0%, rgba(20, 20, 22, 0.8) 100%);
  transition: all var(--transition-base);
}

.ink-upload-zone:hover,
.ink-upload-active {
  border-color: var(--forest-500);
  background: rgba(16, 185, 129, 0.05);
}

.ink-upload-disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.ink-upload-icon {
  width: 20px;
  height: 20px;
  margin: 0 auto var(--space-2);
  background: var(--ink-slate);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--forest-400);
  transition: all var(--transition-base);
}

.ink-upload-icon svg {
  width: 12px;
  height: 12px;
}

.ink-upload-active .ink-upload-icon {
  background: rgba(16, 185, 129, 0.2);
  transform: scale(1.1);
}

.ink-upload-pulse {
  animation: pulse-glow 1.5s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4);
  }
  50% {
    box-shadow: 0 0 0 20px rgba(16, 185, 129, 0);
  }
}

.ink-upload-title {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--paper-white);
  margin-bottom: var(--space-2);
}

.ink-upload-hint {
  color: var(--stone-500);
  font-size: var(--text-sm);
  margin-bottom: var(--space-4);
}

.ink-upload-formats {
  display: flex;
  justify-content: center;
  gap: var(--space-2);
}

/* Upload Progress */
.ink-upload-progress {
  margin-top: var(--space-6);
}

.ink-progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-3);
}

.ink-progress-text {
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--forest-400);
}

.ink-progress-sm {
  height: 4px;
}

/* Files List */
.ink-files-list {
  margin-top: var(--space-6);
}

.ink-file-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--ink-charcoal);
  border-radius: var(--radius-xl);
  margin-bottom: var(--space-3);
}

.ink-file-preview {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--ink-slate);
}

.ink-file-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ink-file-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--stone-500);
}

.ink-file-placeholder svg {
  width: 28px;
  height: 28px;
}

.ink-file-info {
  flex: 1;
  min-width: 0;
}

.ink-file-name {
  font-weight: 500;
  color: var(--paper-white);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ink-file-size {
  font-size: var(--text-sm);
  color: var(--stone-500);
  margin-top: var(--space-1);
}

.ink-file-saved {
  color: var(--forest-400);
}

.ink-file-progress {
  margin-top: var(--space-2);
}

.ink-file-progress-bar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.ink-file-status {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.ink-file-status-success {
  color: var(--forest-400);
}

.ink-file-status-success svg {
  width: 16px;
  height: 16px;
}

.ink-file-status-error {
  color: #f87171;
}

.ink-file-status-pending {
  color: var(--stone-500);
}

.ink-icon-btn-sm {
  width: 32px;
  height: 32px;
}

.ink-icon-btn-sm svg {
  width: 16px;
  height: 16px;
}

.ink-btn-xs {
  padding: var(--space-1) var(--space-2);
  font-size: var(--text-xs);
}

/* Filter Bar */
.ink-filter-bar {
  margin-top: var(--space-6);
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
  min-width: 200px;
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
  width: 160px;
}

.ink-sort-toggle {
  display: flex;
  background: var(--ink-charcoal);
  border: 1px solid var(--ghost-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.ink-sort-btn {
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  color: var(--stone-500);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.ink-sort-active {
  background: var(--forest-600);
  color: var(--paper-white);
}

/* Images Section */
.ink-images-section {
  margin-top: var(--space-6);
}

.ink-images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--space-4);
  margin-top: var(--space-4);
}

.ink-image-card {
  background: var(--ink-charcoal);
  border: 1px solid var(--ghost-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
  transition: all var(--transition-base);
}

.ink-image-card:hover {
  transform: translateY(-4px);
  border-color: rgba(16, 185, 129, 0.3);
  box-shadow: var(--shadow-lg);
}

.ink-image-wrapper {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  cursor: pointer;
}

.ink-image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-slow);
}

.ink-image-card:hover .ink-image-wrapper img {
  transform: scale(1.05);
}

.ink-image-overlay {
  position: absolute;
  inset: 0;
  background: rgba(10, 10, 11, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  opacity: 0;
  transition: opacity var(--transition-base);
}

.ink-image-card:hover .ink-image-overlay {
  opacity: 1;
}

.ink-overlay-btn {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-lg);
  background: var(--ink-slate);
  border: 1px solid var(--ghost-border);
  color: var(--paper-white);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.ink-overlay-btn:hover {
  background: var(--forest-600);
  border-color: var(--forest-500);
}

.ink-overlay-btn-danger:hover {
  background: #dc2626;
  border-color: #ef4444;
}

.ink-overlay-btn svg {
  width: 18px;
  height: 18px;
}

.ink-image-meta {
  padding: var(--space-3);
}

.ink-image-name {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--paper-white);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: var(--space-2);
}

.ink-image-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ink-image-size {
  font-size: var(--text-xs);
  color: var(--stone-500);
}

/* Empty State */
.ink-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16) 0;
  color: var(--stone-500);
}

.ink-empty-state svg {
  width: 64px;
  height: 64px;
  margin-bottom: var(--space-4);
  opacity: 0.5;
}

.ink-empty-hint {
  font-size: var(--text-sm);
  color: var(--stone-700);
  margin-top: var(--space-2);
}

/* Pagination */
.ink-pagination {
  margin-top: var(--space-6);
  display: flex;
  justify-content: center;
}

/* Responsive */
@media (max-width: 768px) {
  .ink-filter-group {
    flex-direction: column;
    align-items: stretch;
  }

  .ink-select {
    width: 100%;
  }

  .ink-images-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
