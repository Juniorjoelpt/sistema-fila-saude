import type { ReactNode } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const LINKS = [
  { to: '/admin', label: 'Dashboard', end: true },
  { to: '/admin/fila', label: 'Fila de Regulação', end: false },
  { to: '/admin/novo-protocolo', label: 'Novo Protocolo', end: false },
  { to: '/admin/cadastros', label: 'Cadastros', end: false },
]

export function AdminLayout({ children }: { children: ReactNode }) {
  const { usuario, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/admin/login')
  }

  return (
    <div className="min-h-screen flex">
      <aside className="w-64 bg-brand-navy text-white flex flex-col shrink-0">
        <div className="px-6 py-5 border-b border-white/10">
          <p className="text-lg font-bold">Fila Saúde</p>
          <p className="text-xs text-white/60">Painel Administrativo</p>
        </div>
        <nav className="flex-1 px-3 py-4 space-y-1">
          {LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) =>
                `block rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                  isActive ? 'bg-brand-teal text-white' : 'text-white/80 hover:bg-white/10'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="px-4 py-4 border-t border-white/10 text-sm">
          <p className="font-semibold">{usuario?.nome}</p>
          <p className="text-white/50 text-xs mb-3">{usuario?.papel}</p>
          <button
            onClick={handleLogout}
            className="w-full rounded-lg bg-white/10 hover:bg-white/20 py-1.5 text-xs font-medium transition-colors"
          >
            Sair
          </button>
        </div>
      </aside>
      <main className="flex-1 p-8 overflow-y-auto">{children}</main>
    </div>
  )
}
