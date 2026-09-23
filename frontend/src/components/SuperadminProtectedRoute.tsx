import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useSuperadminAuth } from '../context/SuperadminAuthContext'

export function SuperadminProtectedRoute({ children }: { children: ReactNode }) {
  const { autenticado } = useSuperadminAuth()

  if (!autenticado) {
    return <Navigate to="/superadmin/login" replace />
  }

  return <>{children}</>
}
