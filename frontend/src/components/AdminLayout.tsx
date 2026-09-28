import { useState, type ReactNode } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard,
  ListOrdered,
  FilePlus2,
  Building2,
  PieChart,
  CalendarClock,
  ShieldCheck,
  Plug,
  Users,
  LogOut,
  Menu,
  X,
  type LucideIcon,
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useTenantBranding } from '../context/TenantBrandingContext'

type LinkDef = { to: string; label: string; end: boolean; icon: LucideIcon }

const LINKS: LinkDef[] = [
  { to: '/admin', label: 'Dashboard', end: true, icon: LayoutDashboard },
  { to: '/admin/fila', label: 'Fila de Regulação', end: false, icon: ListOrdered },
  { to: '/admin/novo-protocolo', label: 'Novo Protocolo', end: false, icon: FilePlus2 },
  { to: '/admin/cadastros', label: 'Cadastros', end: false, icon: Building2 },
]

// O ACS (item 2 do levantamento de requisitos: "cadastra e acompanha
// pacientes da sua área") não tem acesso a regulação de fila, cotas ou
// visão gerencial -- sua navegação fica restrita a uma área própria.
const LINKS_ACS: LinkDef[] = [
  { to: '/admin/meus-pacientes', label: 'Meus Pacientes', end: true, icon: Users },
  { to: '/admin/novo-protocolo', label: 'Novo Protocolo', end: false, icon: FilePlus2 },
]

const LINK_COTAS: LinkDef = { to: '/admin/cotas', label: 'Cotas', end: false, icon: PieChart }
const LINK_AGENDA: LinkDef = { to: '/admin/agenda', label: 'Agenda', end: false, icon: CalendarClock }
const LINK_AUDITORIA: LinkDef = { to: '/admin/auditoria', label: 'Auditoria', end: false, icon: ShieldCheck }
const LINK_INTEGRACOES: LinkDef = { to: '/admin/integracoes', label: 'Integrações', end: false, icon: Plug }
// Configuração da própria conta (2FA) -- aparece pra todo mundo, não é uma
// visão gerencial como as demais.
const LINK_SEGURANCA: LinkDef = { to: '/admin/seguranca', label: 'Segurança', end: false, icon: ShieldCheck }

export function AdminLayout({ children }: { children: ReactNode }) {
  const { usuario, logout } = useAuth()
  const branding = useTenantBranding()
  const navigate = useNavigate()
  const [menuAberto, setMenuAberto] = useState(false)

  let links: LinkDef[]
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
    <div className="min-h-screen lg:flex">
      {/* Barra superior apenas no mobile/tablet: abre o menu lateral. */}
      <div className="lg:hidden sticky top-0 z-30 flex items-center justify-between px-4 py-3 bg-brand-navy text-white shadow-md">
        <div className="flex items-center gap-2">
          <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-teal text-white text-xs font-black">
            FS
          </span>
          <span className="font-bold text-sm">Fila Saúde</span>
        </div>
        <button
          onClick={() => setMenuAberto(true)}
          className="p-1.5 rounded-lg hover:bg-white/10"
          aria-label="Abrir menu"
        >
          <Menu className="h-5 w-5" />
        </button>
      </div>

      {/* Overlay escurecido atrás do menu, só quando aberto no mobile. */}
      {menuAberto && (
        <div
          className="fixed inset-0 z-40 bg-gray-900/50 lg:hidden"
          onClick={() => setMenuAberto(false)}
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 text-white flex flex-col shrink-0 transition-transform duration-300 lg:sticky lg:top-0 lg:h-screen lg:translate-x-0 ${
          menuAberto ? 'translate-x-0' : '-translate-x-full'
        }`}
        style={{
          background: 'linear-gradient(180deg, var(--color-brand-navy) 0%, var(--color-brand-navy-dark) 100%)',
          boxShadow: 'var(--shadow-xl)',
        }}
      >
        <div className="px-6 py-6 border-b border-white/10 flex items-start justify-between">
          <div className="min-w-0">
            {branding.logoUrl ? (
              <img
                src={branding.logoUrl}
                alt={branding.nomeMunicipio ?? 'Logo da Secretaria'}
                className="h-8 max-w-full object-contain mb-1"
              />
            ) : (
              <div className="flex items-center gap-2 mb-1">
                <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-brand-teal text-white text-sm font-black shadow-sm">
                  FS
                </span>
                <p className="text-lg font-bold tracking-tight">Fila Saúde</p>
              </div>
            )}
            <p className="text-xs text-white/55">
              {branding.nomeMunicipio ? `${branding.nomeMunicipio} — Painel Administrativo` : 'Painel Administrativo'}
            </p>
          </div>
          <button
            onClick={() => setMenuAberto(false)}
            className="lg:hidden p-1 rounded-lg hover:bg-white/10 shrink-0"
            aria-label="Fechar menu"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {links.map((link) => {
            const Icon = link.icon
            return (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                onClick={() => setMenuAberto(false)}
                className={({ isActive }) =>
                  `relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-200 ${
                    isActive
                      ? 'bg-white/12 text-white shadow-inner'
                      : 'text-white/65 hover:bg-white/[0.07] hover:text-white'
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
                    <Icon className="h-4 w-4 shrink-0" strokeWidth={2} />
                    <span>{link.label}</span>
                  </>
                )}
              </NavLink>
            )
          })}
        </nav>
        <div className="px-4 py-4 border-t border-white/10 text-sm">
          <div className="flex items-center gap-2.5 mb-3">
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-teal text-xs font-bold text-white shadow-sm">
              {iniciais}
            </span>
            <div className="min-w-0">
              <p className="font-semibold truncate">{usuario?.nome}</p>
              <p className="text-white/50 text-xs">{usuario?.papel}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-1.5 rounded-lg bg-white/10 hover:bg-white/20 py-1.5 text-xs font-medium"
          >
            <LogOut className="h-3.5 w-3.5" strokeWidth={2} />
            Sair
          </button>
        </div>
      </aside>
      <main className="flex-1 min-w-0 p-4 sm:p-6 lg:p-8 overflow-y-auto animate-fade-in">{children}</main>
    </div>
  )
}
