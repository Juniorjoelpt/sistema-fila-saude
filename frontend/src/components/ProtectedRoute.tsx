import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import type { Papel } from '../api/types'

/**
 * Bug de revisão corrigido: antes só checava "está logado", sem olhar o papel
 * -- um ACS que digitasse /admin/cotas, /admin/auditoria ou /admin/integracoes
 * direto na URL via o "esqueleto" da tela administrativa renderizado (o
 * backend já bloqueava os dados, então não havia vazamento real, mas expunha
 * uma UI que não deveria ver). Rotas sensíveis agora podem passar
 * `papeisPermitidos` para bloquear também no frontend.
 */
export function ProtectedRoute({
  children,
  papeisPermitidos,
}: {
  children: ReactNode
  papeisPermitidos?: Papel[]
}) {
  const { autenticado, usuario } = useAuth()

  if (!autenticado) {
    return <Navigate to="/admin/login" replace />
  }

  if (papeisPermitidos && usuario && !papeisPermitidos.includes(usuario.papel)) {
    return <Navigate to="/admin" replace />
  }

  return <>{children}</>
}
