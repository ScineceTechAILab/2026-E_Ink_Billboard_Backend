import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8083',
  timeout: 30000
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token && config.headers) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code === 200) {
      return res
    } else if (res.code === 401) {
      const userStore = useUserStore()
      userStore.logout()
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(new Error(res.info || 'Unauthorized'))
    } else if (res.code === 403) {
      ElMessage.error('没有权限执行此操作')
      return Promise.reject(new Error(res.info || 'Forbidden'))
    } else if (res.code === 500) {
      ElMessage.error('服务器错误，请稍后重试')
      return Promise.reject(new Error(res.info || 'Server Error'))
    } else {
      ElMessage.error(res.info || '请求失败')
      return Promise.reject(new Error(res.info || 'Error'))
    }
  },
  (error) => {
    console.error('Response error:', error)
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
    } else if (error.response?.status === 403) {
      ElMessage.error('没有权限执行此操作')
    } else if (error.response?.status === 500) {
      ElMessage.error('服务器错误，请稍后重试')
    } else {
      ElMessage.error(error.message || '网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default service