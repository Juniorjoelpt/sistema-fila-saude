import type { ReactNode } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTenantBranding } from '../context/TenantBrandingContext'

const LINKS = [
  { to: '/admin', label: 'Dashboard', end: true },
  { to: '/admin/fila', label: 'Fila de Regulação', end: false },
  { to: '/admin/novo-protocolo', label: 'Novo Protocolo', end: false },
  { to: '/admin/cadastros', label: 'Cadastros', end: false },
]

// O ACS (item 2 do levantamento de requisitos: "cadastra e acompanha
// pacientes da sua área") não tem acesso a regulação de fila, cotas ou
// visão gerencial -- sua navegação fica restrita a uma área própria.
const LINKS_ACS = [
  { to: '/admin/meus-pacientes', label: 'Meus Pacientes', end: true },
  { to: '/admin/novo-protocolo', label: 'Novo Protocolo', end: false },
]

const LINK_COTAS = { to: '/admin/cotas', label: 'Cotas', end: false }
const LINK_AGENDA = { to: '/admin/agenda', label: 'Agenda', end: false }
const LINK_AUDITORIA = { to: '/admin/auditoria', label: 'Auditoria', end: false }
const LINK_INTEGRACOES = { to: '/admin/integracoes', label: 'Integrações', end: false }
// Configuração da própria conta (2FA) -- aparece pra todo mundo, não é uma
// visão gerencial como as demais.
const LINK_SEGURANCA = { to: '/admin/seguranca', label: 'Segurança', end: false }

export function AdminLayout({ children }: { children: ReactNode }) {
  const { usuario, logout } = useAuth()
  const branding = useTenantBranding()
  const navigate = useNavigate()

  let links: typeof LINKS
  if (usuario?.papel === 'ACS') {
    links = [...LINKS_ACS, LINK_SEGURANCA]
  } else {
    links = [...LINKS, LINK_COTAS, LINK_AGENDA]
    if (usuario?.papel === 'ADMIN') {
      links = [...links, LINK_AUDITORIA, LINK_INTEGRACOES]
    }
    links = [...links, LINK_SEGURANCA]
  }

  function handleLogout() {
    logout()
    navigate('/admin/login')
  }

  const iniciais = (usuario?.nome ?? '?')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase())
    .join('')

  return (
    <div className="min-h-screen flex">
      <aside className="w-64 bg-brand-navy text-white flex flex-col shrink-0 shadow-xl">
        <div className="px-6 py-5 border-b border-white/10">
          {branding.logoUrl ? (
            <img
              src={branding.logoUrl}
              alt={branding.nomeMunicipio ?? 'Logo da Secretaria'}
              className="h-8 max-w-full object-contain mb-1"
            />
          ) : (
            <p className="text-lg font-bold tracking-tight">Fila Saúde</p>
          )}
          <p className="text-xs text-white/60">
            {branding.nomeMunicipio ? `${branding.nomeMunicipio} — Painel Administrativo` : 'Painel Administrativo'}
          </p>
        </div>
        <nav className="flex-1 px-3 py-4 space-y-1">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) =>
                `relative block rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200 ${
                  isActive
                    ? 'bg-white/10 text-white shadow-inner'
                    : 'text-white/70 hover:bg-white/[0.06] hover:text-white hover:pl-4'
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <span
                    className={`absolute left-0 top-1/2 -translate-y-1/2 h-5 w-[3px] rounded-full bg-brand-teal transition-all duration-200 ${
                      isActive ? 'opacity-100 scale-100' : 'opacity-0 scale-0'
                    }`}
                  />
                  {link.label}
                </>
              )}
            </NavLink>
          ))}
        </nav>
        <div className="px-4 py-4 border-t border-white/10 text-sm">
          <div className="flex items-center gap-2.5 mb-3">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-teal text-xs font-bold text-white">
              {iniciais}
            </span>
            <div className="min-w-0">
              <p className="font-semibold truncate">{usuario?.nome}</p>
              <p className="text-white/50 text-xs">{usuario?.papel}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="w-full rounded-lg bg-white/10 hover:bg-white/20 py-1.5 text-xs font-medium"
          >
            Sair
          </button>
        </div>
      </aside>
      <main className="flex-1 p-8 overflow-y-auto animate-fade-in">{children}</main>
    </div>
  )
}
