<template>
  <div class="ink-audit-page">
    <!-- Background -->
    <div class="ink-grid-bg" />

    <!-- Header -->
    <header class="ink-page-header">
      <div class="ink-container">
        <div class="ink-header-content">
          <div class="ink-header-brand">
            <div class="ink-header-icon ink-glow-box">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h1 class="ink-heading ink-heading-4">内容审核</h1>
              <p class="ink-body">管理所有内容审核记录</p>
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
                placeholder="搜索文件名或用户名..."
                class="ink-input"
                @input="handleSearch"
              />
            </div>

            <el-select v-model="statusFilter" placeholder="审核状态" clearable class="ink-select" @change="loadAuditList">
              <el-option label="全部" value="" />
              <el-option label="待审核" value="PENDING" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
            </el-select>

            <el-select v-model="typeFilter" placeholder="内容类型" clearable class="ink-select" @change="loadAuditList">
              <el-option label="全部" value="" />
              <el-option label="图片" value="IMAGE" />
              <el-option label="视频" value="VIDEO" />
            </el-select>

            <button @click="loadAuditList" class="ink-btn ink-btn-secondary">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 4 23 10 17 10" />
                <path d="M20.49 15a9 9 0 11-2.12-9.36L23 10" />
              </svg>
              刷新
            </button>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="ink-loading-state">
          <div class="ink-spinner-lg" />
          <p>加载审核列表...</p>
        </div>

        <!-- Empty State -->
        <div v-else-if="filteredAuditList.length === 0" class="ink-card ink-empty-state">
          <div class="ink-empty-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h3 class="ink-empty-title">暂无内容</h3>
          <p class="ink-empty-text">所有内容已处理完毕</p>
        </div>

        <!-- Audit List -->
        <div v-else class="ink-audit-list">
          <div
            v-for="item in filteredAuditList"
            :key="item.id"
            class="ink-audit-card"
          >
            <div class="ink-audit-preview">
              <img v-if="item.contentType === 'IMAGE'" :src="item.originalUrl" :alt="item.fileName" />
              <div v-else class="ink-video-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18" />
                  <line x1="7" y1="2" x2="7" y2="22" />
                  <line x1="17" y1="2" x2="17" y2="22" />
                  <line x1="2" y1="12" x2="22" y2="12" />
                </svg>
              </div>
            </div>

            <div class="ink-audit-info">
              <div class="ink-audit-header">
                <h3 class="ink-audit-name">{{ item.fileName }}</h3>
                <span :class="['ink-badge', getStatusClass(item.auditStatus)]">
                  {{ getStatusText(item.auditStatus) }}
                </span>
              </div>

              <div class="ink-audit-meta">
                <div class="ink-meta-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                    <circle cx="12" cy="7" r="4" />
                  </svg>
                  <span>{{ item.userName }}</span>
                </div>
                <div class="ink-meta-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10" />
                    <polyline points="12 6 12 12 16 14" />
                  </svg>
                  <span>{{ formatDate(item.createTime) }}</span>
                </div>
                <div v-if="item.auditReason" class="ink-meta-item ink-meta-error">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                  <span>{{ item.auditReason }}</span>
                </div>
              </div>
            </div>

            <div class="ink-audit-actions">
              <button @click="viewAuditItem(item)" class="ink-btn ink-btn-secondary ink-btn-sm">
                详情
              </button>
              <template v-if="item.auditStatus === 'PENDING'">
                <button @click="quickApprove(item)" class="ink-btn ink-btn-primary ink-btn-sm">
                  通过
                </button>
                <button @click="quickReject(item)" class="ink-btn ink-btn-danger ink-btn-sm">
                  拒绝
                </button>
              </template>
              <button v-else @click="confirmReAudit(item)" class="ink-btn ink-btn-ghost ink-btn-sm">
                重新审核
              </button>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="!loading && total > size" class="ink-pagination">
          <el-pagination
            v-model:current-page="current"
            v-model:page-size="size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadAuditList"
            @current-change="loadAuditList"
          />
        </div>
      </div>
    </main>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="审核详情" width="700px" class="ink-dialog">
      <div v-if="selectedAuditItem" class="ink-detail-content">
        <div class="ink-detail-preview">
          <img v-if="selectedAuditItem.contentType === 'IMAGE'" :src="selectedAuditItem.originalUrl" :alt="selectedAuditItem.fileName" />
          <div v-else class="ink-video-placeholder-lg">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18" />
              <line x1="7" y1="2" x2="7" y2="22" />
              <line x1="17" y1="2" x2="17" y2="22" />
              <line x1="2" y1="12" x2="22" y2="12" />
            </svg>
          </div>
        </div>

        <div class="ink-detail-info">
          <div class="ink-info-row">
            <span class="ink-info-label">文件名</span>
            <span class="ink-info-value">{{ selectedAuditItem.fileName }}</span>
          </div>
          <div class="ink-info-row">
            <span class="ink-info-label">内容类型</span>
            <span class="ink-badge">{{ selectedAuditItem.contentType === 'IMAGE' ? '图片' : '视频' }}</span>
          </div>
          <div class="ink-info-row">
            <span class="ink-info-label">上传用户</span>
            <span class="ink-info-value">{{ selectedAuditItem.userName }}</span>
          </div>
          <div class="ink-info-row">
            <span class="ink-info-label">当前状态</span>
            <span :class="['ink-badge', getStatusClass(selectedAuditItem.auditStatus)]">
              {{ getStatusText(selectedAuditItem.auditStatus) }}
            </span>
          </div>
          <div class="ink-info-row">
            <span class="ink-info-label">上传时间</span>
            <span class="ink-info-value">{{ formatDate(selectedAuditItem.createTime) }}</span>
          </div>
          <div v-if="selectedAuditItem.auditReason" class="ink-info-row">
            <span class="ink-info-label">审核原因</span>
            <span class="ink-info-value ink-text-error">{{ selectedAuditItem.auditReason }}</span>
          </div>
        </div>

        <!-- Audit History -->
        <div v-if="auditHistory.length > 0" class="ink-history-section">
          <h4 class="ink-section-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="1 4 1 10 7 10" />
              <polyline points="23 20 23 14 17 14" />
              <path d="M20.49 9A9 9 0 005.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0118.36 19.49" />
            </svg>
            审核历史
          </h4>
          <div class="ink-history-list">
            <div v-for="log in auditHistory" :key="log.id" class="ink-history-item">
              <div class="ink-history-header">
                <span class="ink-history-type">{{ log.operationType === 'RE_AUDIT' ? '重新审核' : '审核' }}</span>
                <span class="ink-history-time">{{ formatDate(log.createTime) }}</span>
              </div>
              <div class="ink-history-status">
                <span :class="['ink-badge ink-badge-sm', getStatusClass(log.beforeStatus)]">{{ getStatusText(log.beforeStatus) }}</span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="5" y1="12" x2="19" y2="12" />
                  <polyline points="12 5 19 12 12 19" />
                </svg>
                <span :class="['ink-badge ink-badge-sm', getStatusClass(log.afterStatus)]">{{ getStatusText(log.afterStatus) }}</span>
              </div>
              <div class="ink-history-auditor">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                {{ log.auditorName }}
                <span v-if="log.auditReason" class="ink-history-reason">{{ log.auditReason }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Audit Actions -->
        <div v-if="selectedAuditItem.auditStatus === 'PENDING'" class="ink-audit-operation">
          <h4 class="ink-section-title">审核操作</h4>
          <div v-if="showRejectInput" class="ink-reject-input">
            <textarea
              v-model="rejectReason"
              placeholder="请输入拒绝原因..."
              class="ink-input ink-textarea"
              rows="3"
            />
          </div>
          <div class="ink-operation-actions">
            <button @click="handleApprove" :disabled="submitting" class="ink-btn ink-btn-primary ink-btn-flex">
              <span v-if="submitting" class="ink-spinner" />
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12" />
              </svg>
              通过审核
            </button>
            <button @click="handleReject" :disabled="submitting" class="ink-btn ink-btn-danger ink-btn-flex">
              <span v-if="submitting" class="ink-spinner" />
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
              {{ showRejectInput ? '确认拒绝' : '拒绝' }}
            </button>
          </div>
        </div>

        <!-- Re-audit Button -->
        <div v-else class="ink-audit-operation">
          <button @click="handleReAudit" :disabled="submitting" class="ink-btn ink-btn-primary ink-btn-flex">
            <span v-if="submitting" class="ink-spinner" />
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 4 23 10 17 10" />
              <path d="M20.49 15a9 9 0 11-2.12-9.36L23 10" />
            </svg>
            重新审核
          </button>
        </div>
      </div>
      <template #footer>
        <button @click="detailVisible = false" class="ink-btn ink-btn-secondary">关闭</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import type { AuditItemVO, AuditLogVO, AuditDTO } from '@/types'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const auditList = ref<AuditItemVO[]>([])
const auditHistory = ref<AuditLogVO[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
const typeFilter = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const selectedAuditItem = ref<AuditItemVO | null>(null)
const rejectReason = ref('')
const showRejectInput = ref(false)

const filteredAuditList = computed(() => {
  if (!searchKeyword.value) return auditList.value
  return auditList.value.filter((item) => {
    const keyword = searchKeyword.value.toLowerCase()
    return item.fileName?.toLowerCase().includes(keyword) || item.userName?.toLowerCase().includes(keyword)
  })
})

const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getStatusText = (status: string): string => {
  if (status === 'PENDING') return '待审核'
  if (status === 'APPROVED') return '已通过'
  return '已拒绝'
}

const getStatusClass = (status: string): string => {
  if (status === 'PENDING') return 'ink-badge-warning'
  if (status === 'APPROVED') return 'ink-badge-success'
  return 'ink-badge-error'
}

const loadAuditList = async () => {
  loading.value = true
  try {
    const params: any = {
      current: current.value,
      size: size.value
    }
    if (statusFilter.value) params.auditStatus = statusFilter.value
    if (typeFilter.value) params.contentType = typeFilter.value

    const res = await adminApi.getAuditList(params)

    if (res.code === 200) {
      auditList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.info || '加载失败')
    }
  } catch (error: any) {
    console.error('Load audit list failed:', error)
    ElMessage.error(error.message || '加载审核列表失败')
  } finally {
    loading.value = false
  }
}

const loadAuditHistory = async () => {
  if (!selectedAuditItem.value) return
  try {
    const res = await adminApi.getAuditHistory(selectedAuditItem.value.id, selectedAuditItem.value.contentType)
    if (res.code === 200) {
      auditHistory.value = res.data || []
    }
  } catch (error: any) {
    console.error('Load audit history failed:', error)
  }
}

const handleSearch = () => {}

const viewAuditItem = async (item: AuditItemVO) => {
  selectedAuditItem.value = item
  rejectReason.value = ''
  showRejectInput.value = false
  detailVisible.value = true
  await loadAuditHistory()
}

const quickApprove = (item: AuditItemVO) => {
  ElMessageBox.confirm('确定要通过该内容的审核吗？', '确认通过', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'success'
  })
    .then(async () => {
      await performAudit(item, 'APPROVED', '')
    })
    .catch(() => {})
}

const quickReject = (item: AuditItemVO) => {
  ElMessageBox.prompt('请输入拒绝原因', '拒绝审核', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /.+/,
    inputErrorMessage: '请输入拒绝原因'
  })
    .then(async ({ value }) => {
      await performAudit(item, 'REJECTED', value)
    })
    .catch(() => {})
}

const performAudit = async (item: AuditItemVO, status: 'APPROVED' | 'REJECTED', reason: string) => {
  submitting.value = true
  try {
    const auditData: AuditDTO = {
      contentId: item.id,
      contentType: item.contentType,
      auditStatus: status,
      rejectReason: reason
    }
    await adminApi.auditContent(auditData)
    ElMessage.success(status === 'APPROVED' ? '审核通过' : '已拒绝')
    await loadAuditList()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const confirmReAudit = (item: AuditItemVO) => {
  ElMessageBox.confirm('确定要将该内容重置为待审核状态吗？', '确认重新审核', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      submitting.value = true
      try {
        await adminApi.reAudit({
          contentId: item.id,
          contentType: item.contentType
        })
        ElMessage.success('已重置为待审核')
        await loadAuditList()
      } catch (error: any) {
        ElMessage.error(error.message || '操作失败')
      } finally {
        submitting.value = false
      }
    })
    .catch(() => {})
}

const handleApprove = async () => {
  if (!selectedAuditItem.value) return
  await performAudit(selectedAuditItem.value, 'APPROVED', '')
  detailVisible.value = false
}

const handleReject = async () => {
  if (!selectedAuditItem.value) return
  if (!showRejectInput.value) {
    showRejectInput.value = true
    return
  }
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  await performAudit(selectedAuditItem.value, 'REJECTED', rejectReason.value)
  detailVisible.value = false
}

const handleReAudit = async () => {
  if (!selectedAuditItem.value) return
  submitting.value = true
  try {
    await adminApi.reAudit({
      contentId: selectedAuditItem.value.id,
      contentType: selectedAuditItem.value.contentType
    })
    ElMessage.success('已重置为待审核')
    detailVisible.value = false
    await loadAuditList()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadAuditList()
})
</script>

<style scoped>
@import '../styles/design-system.css';

.ink-audit-page {
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
  min-width: 240px;
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

/* Loading & Empty States */
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

/* Audit List */
.ink-audit-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.ink-audit-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  background: linear-gradient(145deg, var(--ink-graphite) 0%, var(--ink-charcoal) 100%);
  border: 1px solid var(--ghost-border);
  border-radius: var(--radius-2xl);
  transition: all var(--transition-base);
}

.ink-audit-card:hover {
  border-color: rgba(16, 185, 129, 0.2);
  transform: translateX(4px);
}

.ink-audit-preview {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--ink-slate);
}

.ink-audit-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ink-video-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--stone-500);
}

.ink-video-placeholder svg {
  width: 32px;
  height: 32px;
}

.ink-audit-info {
  flex: 1;
  min-width: 0;
}

.ink-audit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.ink-audit-name {
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--paper-white);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ink-audit-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.ink-meta-item {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  color: var(--stone-500);
}

.ink-meta-item svg {
  width: 14px;
  height: 14px;
}

.ink-meta-error {
  color: #f87171;
}

.ink-audit-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  flex-shrink: 0;
}

.ink-btn-sm {
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-xs);
}

.ink-btn-danger {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  color: var(--paper-white);
}

.ink-btn-danger:hover:not(:disabled) {
  box-shadow: 0 4px 14px rgba(220, 38, 38, 0.3);
}

/* Pagination */
.ink-pagination {
  margin-top: var(--space-6);
  display: flex;
  justify-content: center;
}

/* Detail Dialog */
.ink-detail-content {
  padding: var(--space-2);
}

.ink-detail-preview {
  width: 100%;
  height: 200px;
  border-radius: var(--radius-xl);
  overflow: hidden;
  margin-bottom: var(--space-6);
  background: var(--ink-slate);
}

.ink-detail-preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.ink-video-placeholder-lg {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--stone-500);
}

.ink-video-placeholder-lg svg {
  width: 64px;
  height: 64px;
}

.ink-detail-info {
  margin-bottom: var(--space-6);
}

.ink-info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--ghost-border);
}

.ink-info-label {
  font-size: var(--text-sm);
  color: var(--stone-500);
}

.ink-info-value {
  font-size: var(--text-sm);
  color: var(--paper-white);
}

.ink-text-error {
  color: #f87171;
}

.ink-badge-sm {
  padding: var(--space-1) var(--space-2);
  font-size: var(--text-xs);
}

/* History Section */
.ink-history-section {
  margin: var(--space-6) 0;
  padding: var(--space-4);
  background: var(--ink-charcoal);
  border-radius: var(--radius-xl);
}

.ink-section-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--paper-white);
  margin-bottom: var(--space-4);
}

.ink-section-title svg {
  width: 18px;
  height: 18px;
  color: var(--forest-400);
}

.ink-history-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.ink-history-item {
  padding: var(--space-3);
  background: var(--ink-graphite);
  border-radius: var(--radius-lg);
  border-left: 3px solid var(--forest-500);
}

.ink-history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-2);
}

.ink-history-type {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--paper-white);
}

.ink-history-time {
  font-size: var(--text-xs);
  color: var(--stone-500);
}

.ink-history-status {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.ink-history-status svg {
  width: 16px;
  height: 16px;
  color: var(--stone-500);
}

.ink-history-auditor {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--stone-400);
}

.ink-history-auditor svg {
  width: 14px;
  height: 14px;
}

.ink-history-reason {
  color: var(--stone-500);
}

/* Audit Operation */
.ink-audit-operation {
  margin-top: var(--space-6);
  padding-top: var(--space-6);
  border-top: 1px solid var(--ghost-border);
}

.ink-reject-input {
  margin-bottom: var(--space-4);
}

.ink-textarea {
  min-height: 80px;
  resize: vertical;
}

.ink-operation-actions {
  display: flex;
  gap: var(--space-3);
}

.ink-btn-flex {
  flex: 1;
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

/* Responsive */
@media (max-width: 768px) {
  .ink-audit-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .ink-audit-actions {
    flex-direction: row;
    width: 100%;
  }

  .ink-audit-actions .ink-btn {
    flex: 1;
  }

  .ink-filter-group {
    flex-direction: column;
    align-items: stretch;
  }

  .ink-select {
    width: 100%;
  }
}
</style>
