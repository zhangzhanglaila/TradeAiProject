import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const resolveBaseURL = () => {
  const envBase = import.meta.env.VITE_API_BASE_URL
  if (!envBase) return '/api'

  const trimmed = String(envBase).replace(/\/+$/, '')
  if (!trimmed) return '/api'

  return trimmed.endsWith('/api') ? trimmed : `${trimmed}/api`
}

const request = axios.create({
  baseURL: resolveBaseURL(),
  timeout: 30000
})

request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    const res = response.data

    // 兼容两种后端返回：
    // 1) { code, message, data }
    // 2) 直接返回业务数据（无 code/data 包裹）
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code !== 200) {
        ElMessage.error(res.message || '请求失败')
        if (res.code === 401) {
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
        }
        return Promise.reject(new Error(res.message || '请求失败'))
      }
      return res
    }

    return { code: 200, data: res }
  },
  error => {
    const status = error?.response?.status

    if (status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
      return Promise.reject(error)
    }

    const msg = error?.response?.data?.message || error.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
