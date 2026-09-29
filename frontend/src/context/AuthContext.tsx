import { createContext, useContext, useState, type ReactNode } from 'react'
import { api } from '../api/client'
import type { LoginResponse, Papel } from '../api/types'

interface UsuarioLogado {
  nome: string
  email: string
  papel: Papel
}

/** Resultado de uma etapa de login: ou terminou (papel do usuário), ou falta o código do segundo fator. */
export type ResultadoLogin = { concluido: true; papel: Papel } | { concluido: false; loginToken: string }

interface AuthContextValue {
  usuario: UsuarioLogado | null
  autenticado: boolean
  login: (email: string, senha: string) => Promise<ResultadoLogin>
  validarDoisFatores: (loginToken: string, codigo: string) => Promise<Papel>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function carregarUsuarioArmazenado(): UsuarioLogado | null {
  const bruto = localStorage.getItem('filasaude:usuario')
  if (!bruto) return null
  try {
    return JSON.parse(bruto) as UsuarioLogado
  } catch {
    // Bug crítico de revisão corrigido: um valor corrompido em localStorage
    // (extensão de navegador, edição manual, schema de versão antiga) fazia
    // o JSON.parse lançar exceção dentro do inicializador de useState, o que
    // derrubava a aplicação inteira já no primeiro render -- inclusive as
    // telas públicas (consulta de protocolo, confirmação de presença), que
    // não deveriam depender de sessão administrativa nenhuma. Em vez de
    // propagar a exceção, trata como "sem sessão" e limpa o valor inválido.
    localStorage.removeItem('filasaude:usuario')
    localStorage.removeItem('filasaude:token')
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioLogado | null>(carregarUsuarioArmazenado)

  function concluirSessao(data: LoginResponse): Papel {
    const usuarioLogado: UsuarioLogado = { nome: data.nome!, email: data.email!, papel: data.papel! }
    localStorage.setItem('filasaude:token', data.token!)
    localStorage.setItem('filasaude:usuario', JSON.stringify(usuarioLogado))
    setUsuario(usuarioLogado)
    return data.papel!
  }

  async function login(email: string, senha: string): Promise<ResultadoLogin> {
    const { data } = await api.post<LoginResponse>('/api/auth/login', { email, senha })
    if (data.requerDoisFatores) {
      return { concluido: false, loginToken: data.loginToken! }
    }
    return { concluido: true, papel: concluirSessao(data) }
  }

  async function validarDoisFatores(loginToken: string, codigo: string): Promise<Papel> {
    const { data } = await api.post<LoginResponse>('/api/auth/2fa/validar-login', { loginToken, codigo })
    return concluirSessao(data)
  }

  function logout() {
    localStorage.removeItem('filasaude:token')
    localStorage.removeItem('filasaude:usuario')
    setUsuario(null)
  }

  return (
    <AuthContext.Provider value={{ usuario, autenticado: usuario !== null, login, validarDoisFatores, logout }}>
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
