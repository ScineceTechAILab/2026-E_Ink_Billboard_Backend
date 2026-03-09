<template>
  <div class="min-h-screen p-6">
    <div class="max-w-6xl mx-auto space-y-6">
      <div class="card animate-slide-up">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-800 mb-1">
              欢迎回来，{{ userStore.userInfo?.nickname }}！
            </h1>
            <p class="text-gray-500">今天是个管理墨水屏的好日子 🎉</p>
          </div>
          <button @click="logout" class="btn-secondary">
            <span class="mr-2">🚪</span>
            退出登录
          </button>
        </div>
      </div>

      <div class="card animate-slide-up" style="animation-delay: 0.05s">
        <div class="flex items-start gap-4">
          <div class="text-4xl">💬</div>
          <div class="flex-1">
            <div v-if="hitokotoLoading" class="text-gray-400 italic">加载中...</div>
            <div v-else>
              <p class="text-lg text-gray-700 italic mb-2">「{{ hitokoto.hitokoto }}」</p>
              <p class="text-sm text-gray-500 text-right">—— {{ hitokoto.from }}</p>
            </div>
          </div>
          <button @click="loadHitokoto" class="text-gray-400 hover:text-gray-600 transition-colors" title="换一条">
            <i class="fas fa-sync-alt"></i>
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div class="card animate-slide-up" style="animation-delay: 0.1s">
          <div class="flex items-center">
            <div
              class="w-14 h-14 bg-gradient-to-r from-amber-400 to-amber-500 rounded-xl flex items-center justify-center text-white text-2xl mr-4"
            >
              📺
            </div>
            <div>
              <p class="text-gray-500 text-sm">在线设备</p>
              <p class="text-2xl font-bold text-gray-800">{{ stats.onlineDevices }}</p>
            </div>
          </div>
        </div>

        <div class="card animate-slide-up" style="animation-delay: 0.2s">
          <div class="flex items-center">
            <div
              class="w-14 h-14 bg-gradient-to-r from-orange-400 to-orange-500 rounded-xl flex items-center justify-center text-white text-2xl mr-4"
            >
              ⏳
            </div>
            <div>
              <p class="text-gray-500 text-sm">待审核内容</p>
              <p class="text-2xl font-bold text-gray-800">{{ stats.pendingAudits }}</p>
            </div>
          </div>
        </div>

        <div class="card animate-slide-up" style="animation-delay: 0.3s">
          <div class="flex items-center">
            <div
              class="w-14 h-14 bg-gradient-to-r from-green-400 to-green-500 rounded-xl flex items-center justify-center text-white text-2xl mr-4"
            >
              ✅
            </div>
            <div>
              <p class="text-gray-500 text-sm">已通过内容</p>
              <p class="text-2xl font-bold text-gray-800">{{ stats.approvedContent }}</p>
            </div>
          </div>
        </div>

        <div class="card animate-slide-up" style="animation-delay: 0.4s">
          <div class="flex items-center">
            <div
              class="w-14 h-14 bg-gradient-to-r from-rose-400 to-rose-500 rounded-xl flex items-center justify-center text-white text-2xl mr-4"
            >
              🖼️
            </div>
            <div>
              <p class="text-gray-500 text-sm">设备总数</p>
              <p class="text-2xl font-bold text-gray-800">{{ totalDevices }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="card animate-slide-up" style="animation-delay: 0.5s">
          <h2 class="text-lg font-semibold text-gray-800 mb-4">
            <span class="mr-2">📤</span>
            快速操作
          </h2>
          <div class="grid grid-cols-2 gap-4">
            <button @click="goToUpload" class="btn-primary text-center py-4">
              <div class="text-3xl mb-2">📸</div>
              <div>上传图片</div>
            </button>
            <button
              @click="goToPushImage"
              class="btn-primary text-center py-4 bg-gradient-to-r from-blue-500 to-cyan-500"
            >
              <div class="text-3xl mb-2">📤</div>
              <div>图片推送</div>
            </button>
            <button
              @click="goToDevices"
              class="btn-primary text-center py-4 bg-gradient-to-r from-purple-500 to-indigo-500"
            >
              <div class="text-3xl mb-2">📺</div>
              <div>设备管理</div>
            </button>
            <button
              @click="goToAudit"
              class="btn-primary text-center py-4 bg-gradient-to-r from-orange-500 to-amber-500 relative"
            >
              <div class="text-3xl mb-2">✅</div>
              <div>内容审核</div>
              <div v-if="stats.pendingAudits > 0" class="absolute -top-2 -right-2 bg-red-500 text-white text-xs w-6 h-6 rounded-full flex items-center justify-center">
                {{ stats.pendingAudits }}
              </div>
            </button>
            <button
              @click="goToAnalytics"
              class="btn-primary text-center py-4 bg-gradient-to-r from-emerald-500 to-teal-500 col-span-2"
            >
              <div class="text-3xl mb-2">📊</div>
              <div>数据统计</div>
            </button>
          </div>
        </div>

        <div class="card animate-slide-up" style="animation-delay: 0.6s">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold text-gray-800">
              <span class="mr-2">🚩</span>
              公告栏
            </h2>
            <button @click="isEditing = !isEditing" class="btn-secondary text-sm py-1 px-3">
              <i v-if="!isEditing" class="fas fa-edit mr-1"></i>
              <i v-else class="fas fa-times mr-1"></i>
              {{ isEditing ? '取消编辑' : '编辑公告' }}
            </button>
          </div>
          
          <div v-if="!isEditing" class="space-y-3">
            <div v-if="!announcement" class="text-center py-8 text-gray-400">
              <i class="fas fa-sticky-note text-4xl mb-2"></i>
              <p>暂无公告</p>
            </div>
            <div v-else class="prose prose-sm max-w-none">
              <div v-html="announcement" class="text-gray-700 whitespace-pre-wrap"></div>
            </div>
            <div v-if="announcement && updatedAt" class="text-xs text-gray-400 text-right pt-2 border-t border-gray-100">
              最后更新：{{ formatDate(updatedAt) }}
            </div>
          </div>
          
          <div v-else class="space-y-3">
            <el-input
              v-model="editableAnnouncement"
              type="textarea"
              :rows="8"
              placeholder="在此输入公告内容，支持HTML标签，如&lt;b&gt;加粗&lt;/b&gt;、&lt;i&gt;斜体&lt;/i&gt;、&lt;br&gt;换行等"
              class="w-full"
            />
            <div class="flex justify-end gap-2">
              <button @click="cancelEdit" class="btn-secondary">
                取消
              </button>
              <button @click="saveAnnouncement" class="btn-primary">
                <i class="fas fa-save mr-2"></i>
                保存公告
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
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

const ANNOUNCEMENT_KEY = 'dashboard_announcement'
const ANNOUNCEMENT_UPDATED_AT_KEY = 'dashboard_announcement_updated_at'

const devices = ref<DeviceVO[]>([])

const totalDevices = computed(() => devices.value.length)

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

/**
 * 加载随机名人名言
 */
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

/**
 * 从本地存储加载公告
 */
const loadAnnouncement = () => {
  try {
    const savedAnnouncement = localStorage.getItem(ANNOUNCEMENT_KEY)
    const savedUpdatedAt = localStorage.getItem(ANNOUNCEMENT_UPDATED_AT_KEY)
    if (savedAnnouncement) {
      announcement.value = savedAnnouncement
    }
    if (savedUpdatedAt) {
      updatedAt.value = savedUpdatedAt
    }
  } catch (error) {
    console.error('Load announcement failed:', error)
  }
}

/**
 * 保存公告到本地存储
 */
const saveAnnouncement = () => {
  try {
    const now = new Date().toISOString()
    localStorage.setItem(ANNOUNCEMENT_KEY, editableAnnouncement.value)
    localStorage.setItem(ANNOUNCEMENT_UPDATED_AT_KEY, now)
    announcement.value = editableAnnouncement.value
    updatedAt.value = now
    isEditing.value = false
    ElMessage.success('公告保存成功！')
  } catch (error) {
    console.error('Save announcement failed:', error)
    ElMessage.error('公告保存失败，请重试')
  }
}

/**
 * 取消编辑公告
 */
const cancelEdit = () => {
  editableAnnouncement.value = announcement.value
  isEditing.value = false
}

/**
 * 格式化日期显示
 */
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

const goToUpload = () => {
  router.push('/upload')
}

const goToDevices = () => {
  router.push('/devices')
}

const goToAudit = () => {
  router.push('/audit')
}

const goToPushImage = () => {
  router.push('/push-image')
}

const goToAnalytics = () => {
  router.push('/analytics')
}

onMounted(() => {
  loadStats()
  loadDevices()
  loadHitokoto()
  loadAnnouncement()
})
</script>