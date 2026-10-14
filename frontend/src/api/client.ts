import axios from 'axios'
import { resolverTenantId } from './tenant'

const baseURL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export const api = axios.create({ baseURL })

/**
 * Resolve uma URL de asset do tenant (ex.: logo enviado por upload, que a API
 * devolve como caminho relativo tipo "/api/public/tenant/{slug}/logo") contra a
 * URL base da API. URLs já absolutas (http/https, ex.: logo hospedado externamente)
 * são devolvidas como estão.
 */
export function resolverUrlAsset(url: string | null): string | null {
  if (!url) return null
  if (/^https?:\/\//i.test(url)) return url
  return `${baseURL}${url}`
}

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
      // Bug alto de revisão corrigido: um 401 em QUALQUER requisição --
      // inclusive nas rotas públicas do cidadão ("/" e "/confirmar-presenca",
      // que nem exigem login) -- forçava um redirect para /admin/login,
      // tirando o cidadão da tela de consulta de protocolo no meio do uso.
      // Só faz sentido redirecionar quando a pessoa já está numa área
      // autenticada (/admin, exceto a própria tela de login).
      const path = window.location.pathname
      const emAreaAutenticada = path.startsWith('/admin') && !path.startsWith('/admin/login')
      if (emAreaAutenticada) {
        window.location.href = '/admin/login'
      }
    }
    // Senha provisória: o backend só libera a troca de senha (403 nas demais rotas).
    // Cobre o caso de a sessão ter sido restaurada de um estado antigo do navegador.
    if (error.response?.status === 403 && error.response?.data?.erro === 'Troca de senha obrigatória') {
      const usuarioBruto = localStorage.getItem('filasaude:usuario')
      if (usuarioBruto) {
        try {
          localStorage.setItem('filasaude:usuario', JSON.stringify({ ...JSON.parse(usuarioBruto), trocarSenha: true }))
        } catch {
          // valor corrompido: o AuthContext já trata como sem sessão
        }
      }
      if (window.location.pathname !== '/admin/trocar-senha') {
        window.location.href = '/admin/trocar-senha'
      }
    }
    return Promise.reject(error)
  },
)
