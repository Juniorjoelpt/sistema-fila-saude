import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { SuperadminAuthProvider } from './context/SuperadminAuthContext'
import { TenantBrandingProvider } from './context/TenantBrandingContext'
import { TenantDevBadge } from './components/TenantDevBadge'
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
import { Agenda } from './pages/Agenda'
import { Auditoria } from './pages/Auditoria'
import { MeusPacientes } from './pages/MeusPacientes'
import { Integracoes } from './pages/Integracoes'
import { Seguranca } from './pages/Seguranca'
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
                <ProtectedRoute papeisPermitidos={['REGULADOR', 'ADMIN']}>
                  <Fila />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/fila/:id"
              element={
                // Sem restrição de papel aqui de propósito: o ACS acessa o detalhe
                // do PRÓPRIO paciente a partir de "Meus Pacientes" (view-only --
                // o backend já bloqueia as ações de mudar status/prioridade/etapa
                // para quem não é Regulador/Admin, e filtra por ACS responsável).
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
                <ProtectedRoute papeisPermitidos={['REGULADOR', 'ADMIN']}>
                  <Cadastros />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/cotas"
              element={
                <ProtectedRoute papeisPermitidos={['REGULADOR', 'ADMIN']}>
                  <Cotas />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/agenda"
              element={
                <ProtectedRoute papeisPermitidos={['REGULADOR', 'ADMIN']}>
                  <Agenda />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/auditoria"
              element={
                <ProtectedRoute papeisPermitidos={['ADMIN']}>
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
                <ProtectedRoute papeisPermitidos={['ADMIN']}>
                  <Integracoes />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/seguranca"
              element={
                // Sem restrição de papel: 2FA é configuração da própria conta,
                // qualquer usuário autenticado (ACS, Regulador, Admin) mexe só
                // na sua (ver TwoFactorController/TwoFactorService).
                <ProtectedRoute>
                  <Seguranca />
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
          <TenantDevBadge />
        </SuperadminAuthProvider>
      </AuthProvider>
      </TenantBrandingProvider>
    </BrowserRouter>
  )
}
