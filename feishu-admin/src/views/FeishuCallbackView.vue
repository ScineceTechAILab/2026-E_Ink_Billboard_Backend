<template>
  <div
    class="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-amber-100 via-orange-100 to-rose-100"
  >
    <div class="card animate-fade-in max-w-md w-full text-center">
      <div v-if="error" class="space-y-4">
        <div class="text-6xl mb-4">😢</div>
        <h2 class="text-xl font-bold text-gray-800">登录失败</h2>
        <p class="text-gray-500">{{ error }}</p>
        <button @click="retry" class="btn-primary mt-4">
          <span class="mr-2">🔄</span>
          重试
        </button>
      </div>

      <div v-else class="space-y-4">
        <div class="text-6xl mb-4 animate-pulse-soft">⏳</div>
        <h2 class="text-xl font-bold text-gray-800">正在登录...</h2>
        <p class="text-gray-500">请稍候，正在完成飞书授权</p>
        <div class="flex justify-center mt-4">
          <el-icon class="is-loading text-3xl text-amber-500"><Loading /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'

const error = ref('')
const router = useRouter()
const userStore = useUserStore()

const handleCallback = async () => {
  try {
    const urlParams = new URLSearchParams(window.location.search)
    const code = urlParams.get('code')

    if (!code) {
      error.value = '未获取到授权码，请重新登录'
      return
    }

    const res = await authApi.feishuLogin(code)

    if (res.code === 200) {
      userStore.login(res.data.token, {
        nickname: res.data.nickname,
        role: res.data.role
      })

      ElMessage.success(`欢迎回来，${res.data.nickname}！`)
      router.replace('/dashboard')
    }
  } catch (err: any) {
    console.error('Login error:', err)
    if (err.response?.status === 401) {
      error.value = '授权已过期，请重新登录'
    } else if (err.response?.status === 403) {
      error.value = '您没有管理员权限'
    } else if (err.response?.status === 500) {
      error.value = '服务器维护中，请稍后重试'
    } else {
      error.value = err.message || '登录失败，请重试'
    }
  }
}

const retry = () => {
  router.replace('/login')
}

onMounted(() => {
  handleCallback()
})
</script>