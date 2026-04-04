import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      // 跳过 logout 请求的拦截，避免退出登录时死循环
      if (error.config?.url === '/auth/logout') {
        return Promise.reject(error)
      }
      localStorage.removeItem('token')
      const msg = error.response?.data?.message || ''
      const reason = msg.includes('会话已过期') ? 'session_expired' : ''
      const loginUrl = reason ? `/login?reason=${reason}` : '/login'
      window.location.href = loginUrl
    }
    return Promise.reject(error)
  }
)

export default api
