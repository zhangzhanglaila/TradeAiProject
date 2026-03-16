import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const downloadClient = axios.create({
  baseURL: request.defaults.baseURL,
  timeout: request.defaults.timeout
})

downloadClient.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

const getFileNameFromDisposition = (disposition) => {
  if (!disposition) return ''

  // RFC5987 filename*=UTF-8''...
  const starMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (starMatch?.[1]) {
    try {
      return decodeURIComponent(starMatch[1])
    } catch {
      return starMatch[1]
    }
  }

  const match = disposition.match(/filename="?([^";]+)"?/i)
  return match?.[1] || ''
}

export async function downloadFile({ url, params, filename } = {}) {
  if (!url) throw new Error('downloadFile: url 不能为空')

  const res = await downloadClient.get(url, {
    params,
    responseType: 'blob'
  })

  const contentType = String(res.headers?.['content-type'] || '')

  // 后端可能返回 JSON 错误体（被 axios 当 blob 接收）
  if (contentType.includes('application/json')) {
    let msg = '下载失败'
    try {
      const text = await res.data.text()
      const json = JSON.parse(text)
      msg = json?.message || json?.msg || msg
    } catch {
      // ignore
    }
    ElMessage.error(msg)
    throw new Error(msg)
  }

  const disposition = res.headers?.['content-disposition']
  const headerFilename = getFileNameFromDisposition(disposition)
  const finalFilename = filename || headerFilename || 'download.csv'

  const blobUrl = window.URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = blobUrl
  a.download = finalFilename
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(blobUrl)
}
