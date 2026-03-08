<template>
  <div
    class="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-amber-100 via-orange-100 to-rose-100"
  >
    <div class="card animate-bounce-in max-w-md w-full">
      <div class="text-center mb-8">
        <div
          class="w-20 h-20 mx-auto mb-4 bg-gradient-to-r from-amber-400 to-rose-500 rounded-2xl flex items-center justify-center shadow-lg"
        >
          <span class="text-4xl">📺</span>
        </div>
        <h1 class="text-2xl font-bold text-gray-800 mb-2">墨水屏广告牌</h1>
        <p class="text-gray-500">管理员管理后台</p>
      </div>

      <div class="space-y-4">
        <button
          @click="goFeishuAuth"
          class="btn-primary w-full flex items-center justify-center gap-3"
          :disabled="loading"
        >
          <span v-if="!loading" class="text-2xl">🦜</span>
          <el-icon v-else :loading="true" class="text-xl"></el-icon>
          <span>{{ loading ? '正在跳转...' : '使用飞书登录' }}</span>
        </button>
      </div>

      <div class="mt-8 pt-6 border-t border-gray-100">
        <p class="text-xs text-gray-400 text-center">
          登录即表示同意服务条款和隐私政策
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const loading = ref(false)
const router = useRouter()

const buildAuthorizeUrl = (): string => {
  const appId = import.meta.env.VITE_FEISHU_APP_ID
  const redirectUri = encodeURIComponent(import.meta.env.VITE_FEISHU_REDIRECT_URI)
  const state = 'fs_' + Math.random().toString(36).slice(2)

  const url = new URL('https://open.feishu.cn/open-apis/authen/v1/authorize')
  url.searchParams.set('app_id', appId)
  url.searchParams.set('redirect_uri', decodeURIComponent(redirectUri))
  url.searchParams.set('state', state)

  return url.toString()
}

const goFeishuAuth = () => {
  loading.value = true
  window.location.href = buildAuthorizeUrl()
}
</script>