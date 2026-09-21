import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { autenticado } = useAuth()

  if (!autenticado) {
    return <Navigate to="/admin/login" replace />
  }

  return <>{children}</>
}
