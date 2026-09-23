import { createContext, useContext, useState, type ReactNode } from 'react'
import { superadminApi } from '../api/superadminClient'

interface SuperadminLogado {
  nome: string
  email: string
}

interface SuperadminAuthContextValue {
  usuario: SuperadminLogado | null
  autenticado: boolean
  login: (email: string, senha: string) => Promise<void>
  logout: () => void
}

const SuperadminAuthContext = createContext<SuperadminAuthContextValue | undefined>(undefined)

function carregarUsuarioArmazenado(): SuperadminLogado | null {
  const bruto = localStorage.getItem('filasaude:superadmin:usuario')
  return bruto ? (JSON.parse(bruto) as SuperadminLogado) : null
}

export function SuperadminAuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<SuperadminLogado | null>(carregarUsuarioArmazenado)

  async function login(email: string, senha: string) {
    const { data } = await superadminApi.post('/api/superadmin/login', { email, senha })
    const logado: SuperadminLogado = { nome: data.nome, email: data.email }

    localStorage.setItem('filasaude:superadmin:token', data.token)
    localStorage.setItem('filasaude:superadmin:usuario', JSON.stringify(logado))
    setUsuario(logado)
  }

  function logout() {
    localStorage.removeItem('filasaude:superadmin:token')
    localStorage.removeItem('filasaude:superadmin:usuario')
    setUsuario(null)
  }

  return (
    <SuperadminAuthContext.Provider value={{ usuario, autenticado: usuario !== null, login, logout }}>
      {children}
    </SuperadminAuthContext.Provider>
  )
}

export function useSuperadminAuth(): SuperadminAuthContextValue {
  const context = useContext(SuperadminAuthContext)
  if (!context) {
    throw new Error('useSuperadminAuth deve ser usado dentro de um SuperadminAuthProvider')
  }
  return context
}
