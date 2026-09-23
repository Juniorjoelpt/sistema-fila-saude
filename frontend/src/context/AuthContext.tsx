import { createContext, useContext, useState, type ReactNode } from 'react'
import { api } from '../api/client'
import type { LoginResponse, Papel } from '../api/types'

interface UsuarioLogado {
  nome: string
  email: string
  papel: Papel
}

interface AuthContextValue {
  usuario: UsuarioLogado | null
  autenticado: boolean
  login: (email: string, senha: string) => Promise<Papel>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function carregarUsuarioArmazenado(): UsuarioLogado | null {
  const bruto = localStorage.getItem('filasaude:usuario')
  return bruto ? (JSON.parse(bruto) as UsuarioLogado) : null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioLogado | null>(carregarUsuarioArmazenado)

  async function login(email: string, senha: string) {
    const { data } = await api.post<LoginResponse>('/api/auth/login', { email, senha })
    const usuarioLogado: UsuarioLogado = { nome: data.nome, email: data.email, papel: data.papel }

    localStorage.setItem('filasaude:token', data.token)
    localStorage.setItem('filasaude:usuario', JSON.stringify(usuarioLogado))
    setUsuario(usuarioLogado)
    return data.papel
  }

  function logout() {
    localStorage.removeItem('filasaude:token')
    localStorage.removeItem('filasaude:usuario')
    setUsuario(null)
  }

  return (
    <AuthContext.Provider value={{ usuario, autenticado: usuario !== null, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider')
  }
  return context
}
