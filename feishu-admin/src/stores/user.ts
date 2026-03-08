import { defineStore } from 'pinia'
import { ref } from 'vue'
import SecureLS from 'secure-ls'
import type { UserState } from '@/types'

const secureLs = new SecureLS({ encodingType: 'aes' })

export const useUserStore = defineStore('user', () => {
  const isAuthenticated = ref(false)
  const token = ref<string | null>(null)
  const userInfo = ref<UserState['userInfo']>(null)

  function init() {
    try {
      const savedToken = secureLs.get('token')
      const savedUserInfo = secureLs.get('userInfo')
      if (savedToken) {
        token.value = savedToken
        isAuthenticated.value = true
      }
      if (savedUserInfo) {
        userInfo.value = savedUserInfo
      }
    } catch (e) {
      console.error('Failed to init user store:', e)
      logout()
    }
  }

  function login(newToken: string, info: { nickname: string; role: string }) {
    token.value = newToken
    userInfo.value = info
    isAuthenticated.value = true
    secureLs.set('token', newToken)
    secureLs.set('userInfo', info)
  }

  function logout() {
    token.value = null
    userInfo.value = null
    isAuthenticated.value = false
    secureLs.remove('token')
    secureLs.remove('userInfo')
  }

  return {
    isAuthenticated,
    token,
    userInfo,
    init,
    login,
    logout
  }
})