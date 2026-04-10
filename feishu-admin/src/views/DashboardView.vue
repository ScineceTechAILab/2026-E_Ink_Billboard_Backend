<template>
  <div class="ink-dashboard">
    <!-- Background -->
    <div class="ink-grid-bg" />

    <!-- Header -->
    <header class="ink-dashboard-header">
      <div class="ink-container">
        <div class="ink-header-content">
          <div class="ink-header-brand">
            <div class="ink-header-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" />
                <path d="M3 9h18M9 21V9" />
              </svg>
            </div>
            <div>
              <h1 class="ink-header-title">墨水屏管理后台</h1>
              <p class="ink-header-subtitle">E-Ink Billboard Admin</p>
            </div>
          </div>
          <div class="ink-header-actions">
            <div class="ink-user-info">
              <span class="ink-user-name">{{ userStore.userInfo?.nickname }}</span>
              <div class="ink-avatar">
                {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
              </div>
            </div>
            <button @click="logout" class="ink-icon-btn ink-tooltip" data-tooltip="退出登录">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="ink-dashboard-main">
      <div class="ink-container">
        <!-- Welcome Card -->
        <div class="ink-card ink-animate-slide-up ink-welcome-card">
          <div class="ink-welcome-content">
            <div>
              <h2 class="ink-heading ink-heading-4">
                欢迎回来，<span class="ink-text-gradient">{{ userStore.userInfo?.nickname }}</span>
              </h2>
              <p class="ink-body ink-mt-2">今天是管理墨水屏的好日子</p>
            </div>
            <div class="ink-welcome-time">
              <div class="ink-time">{{ currentTime }}</div>
              <div class="ink-date">{{ currentDate }}</div>
            </div>
          </div>
        </div>

        <!-- Hitokoto Quote -->
        <div class="ink-card ink-animate-slide-up ink-stagger-1 ink-quote-card">
          <div class="ink-quote-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M14.017 21v-7.391c0-5.704 3.731-9.57 8.983-10.609l.995 2.151c-2.432.917-3.995 3.638-3.995 5.849h4v10h-9.983zm-14.017 0v-7.391c0-5.704 3.748-9.57 9-10.609l.996 2.151c-2.433.917-3.996 3.638-3.996 5.849h3.983v10h-9.983z"/>
            </svg>
          </div>
          <div v-if="hitokotoLoading" class="ink-shimmer ink-quote-shimmer" />
          <div v-else class="ink-quote-content">
            <p class="ink-quote-text">{{ hitokoto.hitokoto }}</p>
            <p class="ink-quote-author">—— {{ hitokoto.from }}</p>
          </div>
          <button @click="loadHitokoto" class="ink-icon-btn ink-quote-refresh" title="换一条">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6M1 20v-6h6M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
            </svg>
          </button>
        </div>

        <!-- Stats Grid -->
        <div class="ink-grid ink-grid-4 ink-mt-6">
          <div class="ink-stat-card ink-animate-slide-up ink-stagger-2">
            <div class="ink-stat-header">
              <div class="ink-stat-icon" style="color: var(--forest-400); background: rgba(16, 185, 129, 0.1);">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                  <line x1="8" y1="21" x2="16" y2="21" />
                  <line x1="12" y1="17" x2="12" y2="21" />
                </svg>
              </div>
              <span class="ink-badge ink-badge-success">实时</span>
            </div>
            <div class="ink-stat-value">{{ stats.onlineDevices }}</div>
            <div class="ink-stat-label">在线设备</div>
          </div>

          <div class="ink-stat-card ink-animate-slide-up ink-stagger-3">
            <div class="ink-stat-header">
              <div class="ink-stat-icon" style="color: #fbbf24; background: rgba(251, 191, 36, 0.1);">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
              </div>
              <span v-if="stats.pendingAudits > 0" class="ink-badge ink-badge-warning">{{ stats.pendingAudits }} 待审</span>
            </div>
            <div class="ink-stat-value">{{ stats.pendingAudits }}</div>
            <div class="ink-stat-label">待审核内容</div>
          </div>

          <div class="ink-stat-card ink-animate-slide-up ink-stagger-4">
            <div class="ink-stat-header">
              <div class="ink-stat-icon" style="color: var(--forest-400); background: rgba(16, 185, 129, 0.1);">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                  <polyline points="22 4 12 14.01 9 11.01" />
                </svg>
              </div>
            </div>
            <div class="ink-stat-value">{{ stats.approvedContent }}</div>
            <div class="ink-stat-label">已通过内容</div>
          </div>

          <div class="ink-stat-card ink-animate-slide-up ink-stagger-5">
            <div class="ink-stat-header">
              <div class="ink-stat-icon" style="color: #67e8f9; background: rgba(6, 182, 212, 0.1);">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <polyline points="21 15 16 10 5 21" />
                </svg>
              </div>
            </div>
            <div class="ink-stat-value">{{ totalDevices }}</div>
            <div class="ink-stat-label">设备总数</div>
          </div>
        </div>

        <!-- Quick Actions & Announcement -->
        <div class="ink-grid ink-grid-2 ink-mt-6">
          <!-- Quick Actions -->
          <div class="ink-card ink-animate-slide-up ink-stagger-6">
            <h3 class="ink-card-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
              </svg>
              快速操作
            </h3>
            <div class="ink-actions-grid">
              <button @click="goToUpload" class="ink-action-btn">
                <div class="ink-action-icon" style="background: linear-gradient(135deg, #10b981, #059669);">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12" />
                  </svg>
                </div>
                <span>上传图片</span>
              </button>

              <button @click="goToPushImage" class="ink-action-btn">
                <div class="ink-action-icon" style="background: linear-gradient(135deg, #06b6d4, #0891b2);">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="22" y1="2" x2="11" y2="13" />
                    <polygon points="22 2 15 22 11 13 2 9 22 2" />
                  </svg>
                </div>
                <span>图片推送</span>
              </button>

              <button @click="goToDevices" class="ink-action-btn">
                <div class="ink-action-icon" style="background: linear-gradient(135deg, #8b5cf6, #7c3aed);">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                    <line x1="8" y1="21" x2="16" y2="21" />
                    <line x1="12" y1="17" x2="12" y2="21" />
                  </svg>
                </div>
                <span>设备管理</span>
              </button>

              <button @click="goToAudit" class="ink-action-btn">
                <div class="ink-action-icon" style="background: linear-gradient(135deg, #f59e0b, #d97706);">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <span>内容审核</span>
                <span v-if="stats.pendingAudits > 0" class="ink-action-badge">{{ stats.pendingAudits }}</span>
              </button>

              <button @click="goToAnalytics" class="ink-action-btn ink-action-btn-wide">
                <div class="ink-action-icon" style="background: linear-gradient(135deg, #ec4899, #db2777);">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="20" x2="18" y2="10" />
                    <line x1="12" y1="20" x2="12" y2="4" />
                    <line x1="6" y1="20" x2="6" y2="14" />
                  </svg>
                </div>
                <span>数据统计</span>
              </button>
            </div>
          </div>

          <!-- Announcement -->
          <div class="ink-card ink-animate-slide-up ink-stagger-6">
            <div class="ink-card-header">
              <h3 class="ink-card-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
                </svg>
                公告栏
              </h3>
              <button @click="isEditing = !isEditing" class="ink-btn ink-btn-ghost ink-btn-sm">
                <svg v-if="!isEditing" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
                {{ isEditing ? '取消' : '编辑' }}
              </button>
            </div>

            <div v-if="!isEditing" class="ink-announcement-content">
              <div v-if="!announcement" class="ink-empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <p>暂无公告</p>
              </div>
              <div v-else class="ink-announcement-text" v-html="announcement" />
              <div v-if="announcement && updatedAt" class="ink-announcement-meta">
                最后更新：{{ formatDate(updatedAt) }}
              </div>
            </div>

            <div v-else class="ink-announcement-edit">
              <textarea
                v-model="editableAnnouncement"
                class="ink-input ink-textarea"
                rows="8"
                placeholder="在此输入公告内容，支持HTML标签，如<b>加粗</b>、<i>斜体</i>、<br>换行等"
              />
              <div class="ink-announcement-actions">
                <button @click="cancelEdit" class="ink-btn ink-btn-secondary">取消</button>
                <button @click="saveAnnouncement" class="ink-btn ink-btn-primary">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z" />
                    <polyline points="17 21 17 13 7 13 7 21" />
                    <polyline points="7 3 7 8 15 8" />
                  </svg>
                  保存公告
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { adminApi, deviceApi } from '@/api'
import type { DeviceVO } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const stats = ref({
  onlineDevices: 0,
  pendingAudits: 0,
  approvedContent: 0,
  online: 0,
  pending: 0,
  approved: 0
})

const hitokoto = ref({
  hitokoto: '',
  from: ''
})
const hitokotoLoading = ref(false)

const announcement = ref('')
const editableAnnouncement = ref('')
const isEditing = ref(false)
const updatedAt = ref('')

const devices = ref<DeviceVO[]>([])
const currentTime = ref('')
const currentDate = ref('')

const totalDevices = computed(() => devices.value.length)

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
}

const loadStats = async () => {
  try {
    const res = await adminApi.getStats()
    if (res.code === 200) {
      stats.value = res.data
    }
  } catch (error) {
    console.error('Load stats failed:', error)
  }
}

const loadDevices = async () => {
  try {
    const res = await deviceApi.list()
    if (res.code === 200) {
      devices.value = res.data
    }
  } catch (error) {
    console.error('Load devices failed:', error)
  }
}

const loadHitokoto = async () => {
  hitokotoLoading.value = true
  try {
    const res = await fetch('https://v1.hitokoto.cn/?encode=json')
    const data = await res.json()
    hitokoto.value = {
      hitokoto: data.hitokoto,
      from: data.from
    }
  } catch (error) {
    console.error('Load hitokoto failed:', error)
    hitokoto.value = {
      hitokoto: '生活明朗，万物可爱。',
      from: '佚名'
    }
  } finally {
    hitokotoLoading.value = false
  }
}

const loadAnnouncement = async () => {
  try {
    const res = await adminApi.getAnnouncement()
    if (res.code === 200 && res.data) {
      announcement.value = res.data.content || res.data.announcement || ''
      updatedAt.value = res.data.updatedAt || ''
    }
  } catch (error) {
    console.error('Load announcement failed:', error)
  }
}

const saveAnnouncement = async () => {
  try {
    const res = await adminApi.saveAnnouncement(editableAnnouncement.value)
    if (res.code === 200) {
      announcement.value = editableAnnouncement.value
      updatedAt.value = new Date().toISOString()
      isEditing.value = false
      ElMessage.success('公告保存成功！')
      await loadAnnouncement()
    } else {
      ElMessage.error(res.info || '公告保存失败，请重试')
    }
  } catch (error) {
    console.error('Save announcement failed:', error)
    ElMessage.error('公告保存失败，请重试')
  }
}

const cancelEdit = () => {
  editableAnnouncement.value = announcement.value
  isEditing.value = false
}

const formatDate = (dateString: string): string => {
  try {
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (error) {
    return dateString
  }
}

const logout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.replace('/login')
  })
}

const goToUpload = () => router.push('/upload')
const goToDevices = () => router.push('/devices')
const goToAudit = () => router.push('/audit')
const goToPushImage = () => router.push('/push-image')
const goToAnalytics = () => router.push('/analytics')

onMounted(() => {
  updateTime()
  setInterval(updateTime, 1000)
  loadStats()
  loadDevices()
  loadHitokoto()
  loadAnnouncement()
})
</script>

<style scoped>
@import '../styles/design-system.css';

.ink-dashboard {
  min-height: 100vh;
  background: var(--ink-black);
}

/* Header */
.ink-dashboard-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: var(--z-fixed);
  background: rgba(10, 10, 11, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
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
  gap: var(--space-3);
}

.ink-header-icon {
  width: 40px;
  height: 40px;
  color: var(--forest-400);
}

.ink-header-title {
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--paper-white);
}

.ink-header-subtitle {
  font-size: var(--text-xs);
  color: var(--stone-400);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.ink-header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.ink-user-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.ink-user-name {
  font-size: var(--text-sm);
  color: var(--stone-300);
  font-weight: 500;
}

.ink-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--forest-600), var(--forest-700));
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: var(--text-sm);
  color: var(--paper-white);
}

/* Main Content */
.ink-dashboard-main {
  padding-top: 80px;
  padding-bottom: var(--space-8);
}

.ink-mt-2 { margin-top: var(--space-2); }
.ink-mt-6 { margin-top: var(--space-6); }

/* Welcome Card */
.ink-welcome-card {
  margin-bottom: var(--space-6);
}

.ink-welcome-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.ink-welcome-time {
  text-align: right;
}

.ink-time {
  font-family: var(--font-display);
  font-size: var(--text-3xl);
  font-weight: 700;
  color: var(--forest-400);
  line-height: 1;
}

.ink-date {
  font-size: var(--text-sm);
  color: var(--stone-400);
  margin-top: var(--space-1);
}

/* Quote Card */
.ink-quote-card {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.ink-quote-icon {
  width: 32px;
  height: 32px;
  color: var(--forest-500);
  flex-shrink: 0;
  opacity: 0.5;
}

.ink-quote-content {
  flex: 1;
}

.ink-quote-text {
  font-size: var(--text-lg);
  color: var(--paper-white);
  font-style: italic;
  line-height: 1.6;
}

.ink-quote-author {
  font-size: var(--text-sm);
  color: var(--stone-400);
  margin-top: var(--space-2);
  text-align: right;
}

.ink-quote-shimmer {
  flex: 1;
  height: 60px;
  border-radius: var(--radius-lg);
}

.ink-quote-refresh {
  flex-shrink: 0;
}

/* Stats */
.ink-stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.ink-stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ink-stat-icon svg {
  width: 24px;
  height: 24px;
}

/* Card Components */
.ink-card-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--paper-white);
  margin-bottom: var(--space-4);
}

.ink-card-title svg {
  width: 20px;
  height: 20px;
  color: var(--forest-400);
}

.ink-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.ink-card-header .ink-card-title {
  margin-bottom: 0;
}

.ink-btn-sm {
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-xs);
}

/* Quick Actions */
.ink-actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3);
}

.ink-action-btn {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--ink-charcoal);
  border: 1px solid var(--ghost-border);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}

.ink-action-btn:hover {
  background: var(--ink-slate);
  border-color: rgba(16, 185, 129, 0.3);
  transform: translateY(-2px);
}

.ink-action-btn-wide {
  grid-column: span 2;
}

.ink-action-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--paper-white);
  flex-shrink: 0;
}

.ink-action-icon svg {
  width: 20px;
  height: 20px;
}

.ink-action-btn span {
  font-weight: 600;
  color: var(--paper-white) !important;
}

.ink-action-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  background: var(--error);
  color: var(--paper-white);
  font-size: var(--text-xs);
  font-weight: 600;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Announcement */
.ink-announcement-content {
  min-height: 200px;
}

.ink-announcement-text {
  color: var(--ghost-text);
  line-height: 1.8;
  white-space: pre-wrap;
}

.ink-announcement-meta {
  font-size: var(--text-xs);
  color: var(--stone-500);
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--ghost-border);
  text-align: right;
}

.ink-announcement-edit {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.ink-textarea {
  min-height: 160px;
  resize: vertical;
}

.ink-announcement-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.ink-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) 0;
  color: var(--stone-500);
}

.ink-empty-state svg {
  width: 48px;
  height: 48px;
  margin-bottom: var(--space-4);
  opacity: 0.5;
}

/* Responsive */
@media (max-width: 768px) {
  .ink-welcome-content {
    flex-direction: column;
    text-align: center;
  }

  .ink-welcome-time {
    text-align: center;
  }

  .ink-actions-grid {
    grid-template-columns: 1fr;
  }

  .ink-action-btn-wide {
    grid-column: span 1;
  }
}
</style>
