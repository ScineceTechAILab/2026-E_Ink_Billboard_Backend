<template>
  <div class="ink-callback-container">
    <!-- Background -->
    <div class="ink-grid-bg" />
    <div class="ink-particles">
      <div
        v-for="n in 20"
        :key="n"
        class="ink-particle"
        :style="getParticleStyle(n)"
      />
    </div>

    <!-- Main Card -->
    <div class="ink-callback-card" :class="{ 'ink-error': error }">
      <!-- Loading State -->
      <div v-if="!error" class="ink-callback-content">
        <div class="ink-loading-ring">
          <div class="ink-loading-circle" />
          <div class="ink-loading-circle ink-loading-circle-delayed" />
        </div>

        <div class="ink-callback-text">
          <h2 class="ink-heading ink-heading-3">正在登录</h2>
          <p class="ink-body">请稍候，正在完成飞书授权</p>
          <div class="ink-progress-bar">
            <div class="ink-progress-fill" />
          </div>
        </div>
      </div>

      <!-- Error State -->
      <div v-else class="ink-callback-content ink-callback-error">
        <div class="ink-error-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="15" y1="9" x2="9" y2="15" />
            <line x1="9" y1="9" x2="15" y2="15" />
          </svg>
        </div>

        <div class="ink-callback-text">
          <h2 class="ink-heading ink-heading-3 ink-text-error">登录失败</h2>
          <p class="ink-body">{{ error }}</p>
        </div>

        <button @click="retry" class="ink-btn ink-btn-primary ink-btn-lg">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10" />
            <path d="M20.49 15a9 9 0 11-2.12-9.36L23 10" />
          </svg>
          重试
        </button>
      </div>
    </div>

    <!-- Floating Orbs -->
    <div class="ink-orb ink-orb-1" />
    <div class="ink-orb ink-orb-2" />
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

const getParticleStyle = (n: number) => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 20}s`,
  animationDuration: `${15 + Math.random() * 10}s`
})

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

<style scoped>
@import '../styles/design-system.css';

.ink-callback-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--ink-black);
}

.ink-callback-card {
  position: relative;
  z-index: 10;
  width: 400px;
  max-width: 90vw;
  padding: var(--space-10);
  background: linear-gradient(145deg, rgba(28, 28, 31, 0.8) 0%, rgba(20, 20, 22, 0.9) 100%);
  backdrop-filter: blur(20px);
  border: 1px solid var(--ghost-border);
  border-radius: var(--radius-3xl);
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(16, 185, 129, 0.1);
  transition: all var(--transition-base);
}

.ink-error {
  border-color: rgba(239, 68, 68, 0.3);
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(239, 68, 68, 0.1);
}

.ink-callback-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

/* Loading Animation */
.ink-loading-ring {
  position: relative;
  width: 80px;
  height: 80px;
  margin-bottom: var(--space-6);
}

.ink-loading-circle {
  position: absolute;
  inset: 0;
  border: 3px solid transparent;
  border-top-color: var(--forest-500);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.ink-loading-circle-delayed {
  border-top-color: transparent;
  border-right-color: var(--forest-400);
  animation-duration: 1.5s;
  animation-direction: reverse;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.ink-callback-text {
  margin-bottom: var(--space-6);
}

.ink-callback-text h2 {
  margin-bottom: var(--space-2);
}

.ink-progress-bar {
  width: 200px;
  height: 4px;
  background: var(--ink-slate);
  border-radius: var(--radius-full);
  overflow: hidden;
  margin-top: var(--space-4);
}

.ink-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--forest-600), var(--forest-400));
  border-radius: var(--radius-full);
  animation: progress 2s ease-in-out infinite;
}

@keyframes progress {
  0% {
    width: 0%;
    transform: translateX(-100%);
  }
  50% {
    width: 100%;
    transform: translateX(0);
  }
  100% {
    width: 100%;
    transform: translateX(100%);
  }
}

/* Error State */
.ink-error-icon {
  width: 80px;
  height: 80px;
  background: rgba(239, 68, 68, 0.1);
  border: 2px solid rgba(239, 68, 68, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ef4444;
  margin-bottom: var(--space-6);
  animation: shake 0.5s ease-in-out;
}

.ink-error-icon svg {
  width: 40px;
  height: 40px;
}

@keyframes shake {
  0%, 100% {
    transform: translateX(0);
  }
  20%, 60% {
    transform: translateX(-10px);
  }
  40%, 80% {
    transform: translateX(10px);
  }
}

.ink-text-error {
  color: #f87171;
}

.ink-callback-error .ink-body {
  color: #fca5a5;
}

.ink-btn-lg {
  padding: var(--space-3) var(--space-8);
  font-size: var(--text-base);
}

.ink-btn-lg svg {
  width: 20px;
  height: 20px;
}

/* Floating Orbs */
.ink-orb {
  position: fixed;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  pointer-events: none;
  z-index: 1;
}

.ink-orb-1 {
  width: 400px;
  height: 400px;
  background: var(--forest-600);
  top: -100px;
  right: -100px;
  animation: float-orb 20s ease-in-out infinite;
}

.ink-orb-2 {
  width: 300px;
  height: 300px;
  background: var(--forest-800);
  bottom: -50px;
  left: -50px;
  animation: float-orb 25s ease-in-out infinite reverse;
}

@keyframes float-orb {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(50px, -30px) scale(1.1);
  }
  50% {
    transform: translate(-30px, 50px) scale(0.9);
  }
  75% {
    transform: translate(-50px, -20px) scale(1.05);
  }
}

/* Responsive */
@media (max-width: 480px) {
  .ink-callback-card {
    padding: var(--space-6);
  }
}
</style>
