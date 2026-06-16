import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 120000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const data = response.data
    if (data && data.success === false) {
      ElMessage.error(data.error?.message || '请求失败')
      return Promise.reject(new Error(data.error?.message || '请求失败'))
    }
    return response
  },
  (error) => {
    const msg = error.response?.data?.error?.message || error.message || '网络错误'
    if (error.response?.status === 401) {
      localStorage.clear()
      window.location.href = '/login'
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export interface ApiResponse<T> {
  success: boolean
  data: T
  error?: { code: string; message: string }
}

export default http
