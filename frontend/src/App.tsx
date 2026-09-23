import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { SuperadminAuthProvider } from './context/SuperadminAuthContext'
import { TenantBrandingProvider } from './context/TenantBrandingContext'
import { ProtectedRoute } from './components/ProtectedRoute'
import { SuperadminProtectedRoute } from './components/SuperadminProtectedRoute'
import { ConsultaProtocolo } from './pages/ConsultaProtocolo'
import { Login } from './pages/Login'
import { Dashboard } from './pages/Dashboard'
import { Fila } from './pages/Fila'
import { NovoProtocolo } from './pages/NovoProtocolo'
import { ProtocoloDetalhe } from './pages/ProtocoloDetalhe'
import { Cadastros } from './pages/Cadastros'
import { Cotas } from './pages/Cotas'
import { Auditoria } from './pages/Auditoria'
import { MeusPacientes } from './pages/MeusPacientes'
import { Integracoes } from './pages/Integracoes'
import { SuperadminLogin } from './pages/superadmin/SuperadminLogin'
import { SuperadminDashboard } from './pages/superadmin/SuperadminDashboard'

export default function App() {
  return (
    <BrowserRouter>
      <TenantBrandingProvider>
      <AuthProvider>
        <SuperadminAuthProvider>
          <Routes>
            <Route path="/" element={<ConsultaProtocolo />} />
            <Route path="/admin/login" element={<Login />} />
            <Route
              path="/admin"
              element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/fila"
              element={
                <ProtectedRoute>
                  <Fila />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/fila/:id"
              element={
                <ProtectedRoute>
                  <ProtocoloDetalhe />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/novo-protocolo"
              element={
                <ProtectedRoute>
                  <NovoProtocolo />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/cadastros"
              element={
                <ProtectedRoute>
                  <Cadastros />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/cotas"
              element={
                <ProtectedRoute>
                  <Cotas />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/auditoria"
              element={
                <ProtectedRoute>
                  <Auditoria />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/meus-pacientes"
              element={
                <ProtectedRoute>
                  <MeusPacientes />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/integracoes"
              element={
                <ProtectedRoute>
                  <Integracoes />
                </ProtectedRoute>
              }
            />

            <Route path="/superadmin/login" element={<SuperadminLogin />} />
            <Route
              path="/superadmin"
              element={
                <SuperadminProtectedRoute>
                  <SuperadminDashboard />
                </SuperadminProtectedRoute>
              }
            />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </SuperadminAuthProvider>
      </AuthProvider>
      </TenantBrandingProvider>
    </BrowserRouter>
  )
}
