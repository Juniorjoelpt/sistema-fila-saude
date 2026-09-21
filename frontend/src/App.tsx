import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute } from './components/ProtectedRoute'
import { ConsultaProtocolo } from './pages/ConsultaProtocolo'
import { Login } from './pages/Login'
import { Dashboard } from './pages/Dashboard'
import { Fila } from './pages/Fila'
import { NovoProtocolo } from './pages/NovoProtocolo'
import { ProtocoloDetalhe } from './pages/ProtocoloDetalhe'
import { Cadastros } from './pages/Cadastros'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
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
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
