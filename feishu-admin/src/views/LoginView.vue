<template>
  <div class="ink-login-container">
    <!-- Animated Background -->
    <div class="ink-particles">
      <div
        v-for="n in 30"
        :key="n"
        class="ink-particle"
        :style="getParticleStyle(n)"
      />
    </div>
    <div class="ink-grid-bg" />

    <!-- Floating Orbs -->
    <div class="ink-orb ink-orb-1" />
    <div class="ink-orb ink-orb-2" />

    <!-- Main Content -->
    <div class="ink-login-content">
      <div class="ink-login-card ink-animate-scale-in">
        <!-- Logo Section -->
        <div class="ink-logo-section">
          <div class="ink-logo">
            <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="64" height="64" rx="16" fill="url(#logo-gradient)" />
              <path d="M20 20h24v4H20v-4zm0 10h20v4H20v-4zm0 10h16v4H20v-4z" fill="currentColor" />
              <defs>
                <linearGradient id="logo-gradient" x1="0" y1="0" x2="64" y2="64">
                  <stop stop-color="#10b981" />
                  <stop offset="1" stop-color="#059669" />
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h1 class="ink-heading ink-heading-3 ink-text-gradient">墨水屏广告牌</h1>
          <p class="ink-subtitle">管理员控制台</p>
        </div>

        <!-- Login Button -->
        <button
          @click="goFeishuAuth"
          class="ink-btn ink-btn-primary ink-login-btn"
          :disabled="loading"
        >
          <span v-if="loading" class="ink-spinner" />
          <svg v-else class="ink-feishu-icon" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/>
          </svg>
          <span>{{ loading ? '正在跳转...' : '使用飞书登录' }}</span>
        </button>

        <!-- Features -->
        <div class="ink-features">
          <div class="ink-feature-item ink-animate-slide-up ink-stagger-1">
            <div class="ink-feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" />
                <path d="M3 9h18M9 21V9" />
              </svg>
            </div>
            <span>设备管理</span>
          </div>
          <div class="ink-feature-item ink-animate-slide-up ink-stagger-2">
            <div class="ink-feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12" />
              </svg>
            </div>
            <span>内容推送</span>
          </div>
          <div class="ink-feature-item ink-animate-slide-up ink-stagger-3">
            <div class="ink-feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <span>审核管理</span>
          </div>
        </div>

        <!-- Footer -->
        <div class="ink-login-footer">
          <p class="ink-footer-text">登录即表示同意服务条款和隐私政策</p>
          <div class="ink-divider" />
          <p class="ink-copyright">© 2026 STALAB</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const loading = ref(false)

const getParticleStyle = (n: number) => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 20}s`,
  animationDuration: `${15 + Math.random() * 10}s`
})

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

<style scoped>
@import '../styles/design-system.css';

.ink-login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--ink-black);
}

.ink-login-content {
  position: relative;
  z-index: 10;
  padding: var(--space-6);
}

.ink-login-card {
  width: 420px;
  max-width: 90vw;
  padding: var(--space-10);
  background: linear-gradient(145deg, rgba(28, 28, 31, 0.8) 0%, rgba(20, 20, 22, 0.9) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--ghost-border);
  border-radius: var(--radius-3xl);
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(16, 185, 129, 0.1);
}

.ink-logo-section {
  text-align: center;
  margin-bottom: var(--space-8);
}

.ink-logo {
  width: 72px;
  height: 72px;
  margin: 0 auto var(--space-4);
  color: var(--paper-white);
  filter: drop-shadow(0 0 20px rgba(16, 185, 129, 0.3));
}

.ink-subtitle {
  color: var(--stone-500);
  font-size: var(--text-base);
  margin-top: var(--space-2);
}

.ink-login-btn {
  width: 100%;
  padding: var(--space-4) var(--space-6);
  font-size: var(--text-base);
  margin-bottom: var(--space-8);
}

.ink-feishu-icon {
  width: 20px;
  height: 20px;
}

.ink-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.ink-features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-8);
}

.ink-feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4);
  background: var(--ghost-white);
  border-radius: var(--radius-xl);
  border: 1px solid var(--ghost-border);
  transition: all var(--transition-fast);
}

.ink-feature-item:hover {
  background: rgba(16, 185, 129, 0.05);
  border-color: rgba(16, 185, 129, 0.2);
  transform: translateY(-2px);
}

.ink-feature-icon {
  width: 32px;
  height: 32px;
  color: var(--forest-400);
}

.ink-feature-item span {
  font-size: var(--text-xs);
  color: var(--stone-300);
  font-weight: 500;
}

.ink-login-footer {
  text-align: center;
}

.ink-footer-text {
  font-size: var(--text-xs);
  color: var(--stone-500);
}

.ink-copyright {
  font-size: var(--text-xs);
  color: var(--stone-700);
  margin-top: var(--space-4);
}

.ink-divider {
  margin: var(--space-6) 0;
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
  .ink-login-card {
    padding: var(--space-6);
  }

  .ink-features {
    grid-template-columns: 1fr;
    gap: var(--space-3);
  }

  .ink-feature-item {
    flex-direction: row;
    justify-content: flex-start;
  }
}
</style>
