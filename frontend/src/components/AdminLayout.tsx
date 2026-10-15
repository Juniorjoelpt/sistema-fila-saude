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
  Timer,
  Globe,
  LogOut,
  Menu,
  X,
  type LucideIcon,
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useTenantBranding } from '../context/TenantBrandingContext'
import logoSimbolo from '../assets/brand/agenda-saude-simbolo.svg'

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
const LINK_SLA: LinkDef = { to: '/admin/sla-configuracoes', label: 'SLA', end: false, icon: Timer }
const LINK_AGENDA: LinkDef = { to: '/admin/agenda', label: 'Agenda', end: false, icon: CalendarClock }
const LINK_AUDITORIA: LinkDef = { to: '/admin/auditoria', label: 'Auditoria', end: false, icon: ShieldCheck }
const LINK_TRANSPARENCIA: LinkDef = { to: '/admin/transparencia', label: 'Transparência', end: false, icon: Globe }
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
    links = [...LINKS, LINK_COTAS, LINK_SLA, LINK_AGENDA]
    if (usuario?.papel === 'ADMIN') {
      links = [...links, LINK_AUDITORIA, LINK_TRANSPARENCIA, LINK_INTEGRACOES]
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

  const principais = links.slice(0, 4)
  const GLASS = {
    background: 'rgb(255 255 255 / 0.62)',
    WebkitBackdropFilter: 'blur(22px) saturate(1.4)',
    backdropFilter: 'blur(22px) saturate(1.4)',
    border: '1px solid rgb(255 255 255 / 0.8)',
    boxShadow: '0 18px 44px -16px rgb(14 111 104 / 0.28)',
  } as const

  return (
    <div className="min-h-screen lg:flex lg:gap-0">
      {/* Barra superior (mobile/tablet): vidro flutuante. */}
      <div
        className="lg:hidden sticky top-3 z-30 mx-3 mt-3 flex items-center justify-between rounded-full px-4 py-2.5"
        style={GLASS}
      >
        <div className="flex items-center gap-2">
          <img src={logoSimbolo} alt="Agenda Saúde" className="h-7 w-7" />
          <span className="font-bold text-sm text-brand-navy">Agenda Saúde</span>
        </div>
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-teal text-[11px] font-bold text-white">
          {iniciais}
        </span>
      </div>

      {menuAberto && (
        <div
          className="fixed inset-0 z-40 bg-gray-900/40 backdrop-blur-sm lg:hidden"
          onClick={() => setMenuAberto(false)}
        />
      )}

      <aside
        className={`fixed inset-y-3 left-3 z-50 w-64 flex flex-col shrink-0 rounded-[2rem] transition-transform duration-300 lg:sticky lg:top-3 lg:h-[calc(100vh-1.5rem)] lg:m-3 lg:translate-x-0 ${
          menuAberto ? 'translate-x-0' : '-translate-x-[120%]'
        }`}
        style={GLASS}
      >
        <div className="px-6 py-6 flex items-start justify-between">
          <div className="min-w-0">
            {branding.logoUrl ? (
              <img
                src={branding.logoUrl}
                alt={branding.nomeMunicipio ?? 'Logo da Secretaria'}
                className="h-8 max-w-full object-contain mb-1"
              />
            ) : (
              <div className="flex items-center gap-2 mb-1">
                <img src={logoSimbolo} alt="Agenda Saúde" className="h-8 w-8 shrink-0" />
                <p className="text-lg font-bold tracking-tight text-brand-navy">Agenda Saúde</p>
              </div>
            )}
            <p className="text-xs text-gray-500">
              {branding.nomeMunicipio ? `${branding.nomeMunicipio} — Painel Administrativo` : 'Painel Administrativo'}
            </p>
          </div>
          <button
            onClick={() => setMenuAberto(false)}
            className="lg:hidden p-1.5 rounded-full text-gray-500 hover:bg-black/5 shrink-0"
            aria-label="Fechar menu"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <nav className="flex-1 px-3 pb-2 space-y-1 overflow-y-auto">
          {links.map((link) => {
            const Icon = link.icon
            return (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                onClick={() => setMenuAberto(false)}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-full px-4 py-2.5 text-sm font-medium transition-all duration-200 ${
                    isActive ? 'text-white' : 'text-gray-600 hover:bg-white/70 hover:text-brand-navy'
                  }`
                }
                style={({ isActive }) =>
                  isActive
                    ? {
                        background:
                          'linear-gradient(135deg, color-mix(in srgb, var(--color-brand-teal) 80%, white), var(--color-brand-teal))',
                        boxShadow: '0 10px 20px -8px color-mix(in srgb, var(--color-brand-teal) 70%, transparent)',
                      }
                    : undefined
                }
              >
                <Icon className="h-4 w-4 shrink-0" strokeWidth={2} />
                <span>{link.label}</span>
              </NavLink>
            )
          })}
        </nav>
        <div className="m-3 rounded-3xl bg-white/60 p-3 text-sm">
          <div className="flex items-center gap-2.5 mb-3">
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-teal text-xs font-bold text-white shadow-sm">
              {iniciais}
            </span>
            <div className="min-w-0">
              <p className="font-semibold truncate text-gray-800">{usuario?.nome}</p>
              <p className="text-gray-500 text-xs">{usuario?.papel}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-1.5 rounded-full bg-white/80 hover:bg-white py-2 text-xs font-medium text-gray-700"
          >
            <LogOut className="h-3.5 w-3.5" strokeWidth={2} />
            Sair
          </button>
        </div>
      </aside>

      <main className="flex-1 min-w-0 p-4 pb-28 sm:p-6 sm:pb-28 lg:p-8 lg:pb-8 overflow-y-auto animate-fade-in">
        {children}
      </main>

      {/* Navegação inferior flutuante (mobile/tablet). */}
      <nav
        className="lg:hidden fixed bottom-4 left-1/2 z-30 flex -translate-x-1/2 items-center gap-1 rounded-full px-2 py-2"
        style={GLASS}
        aria-label="Navegação principal"
      >
        {principais.map((link) => {
          const Icon = link.icon
          return (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              aria-label={link.label}
              className={({ isActive }) =>
                `flex h-11 w-11 items-center justify-center rounded-full transition-all ${
                  isActive ? 'bg-brand-teal text-white shadow-md' : 'text-gray-500 hover:bg-white/70'
                }`
              }
            >
              <Icon className="h-5 w-5" strokeWidth={2} />
            </NavLink>
          )
        })}
        <button
          onClick={() => setMenuAberto(true)}
          className="flex h-11 w-11 items-center justify-center rounded-full text-gray-500 hover:bg-white/70"
          aria-label="Mais opções"
        >
          <Menu className="h-5 w-5" />
        </button>
      </nav>
    </div>
  )
}
