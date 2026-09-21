import axios from 'axios'
import { resolverTenantId } from './tenant'

const baseURL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export const api = axios.create({ baseURL })

api.interceptors.request.use((config) => {
  config.headers['X-Tenant-Id'] = resolverTenantId()

  const token = localStorage.getItem('filasaude:token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('filasaude:token')
      localStorage.removeItem('filasaude:usuario')
      if (!window.location.pathname.startsWith('/admin/login')) {
        window.location.href = '/admin/login'
      }
    }
    return Promise.reject(error)
  },
)
