import axios from 'axios'
import { useAuthStore } from '../store/authStore'

export const TOKEN_KEY = 'liveticket_access_token'
export const USER_KEY = 'liveticket_user'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response && error.response.status === 401) {
      useAuthStore.getState().logout()
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      if (location.pathname !== '/login') {
        location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default http
