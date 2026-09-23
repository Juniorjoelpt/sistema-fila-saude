import axios from 'axios'

/**
 * Cliente HTTP dedicado ao painel de superadmin (item 3.6, Fase 2). Usa um
 * "tenant" fixo e reservado ("superadmin"), que não corresponde a nenhuma
 * prefeitura real — apenas reaproveita a mesma checagem de tenant do token
 * já existente no backend (ver JwtService.SUPERADMIN_TENANT). Token
 * armazenado sob uma chave própria, independente do login de prefeitura.
 */
const baseURL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export const superadminApi = axios.create({ baseURL })

superadminApi.interceptors.request.use((config) => {
  config.headers['X-Tenant-Id'] = 'superadmin'

  const token = localStorage.getItem('filasaude:superadmin:token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

superadminApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('filasaude:superadmin:token')
      localStorage.removeItem('filasaude:superadmin:usuario')
      if (!window.location.pathname.startsWith('/superadmin/login')) {
        window.location.href = '/superadmin/login'
      }
    }
    return Promise.reject(error)
  },
)
