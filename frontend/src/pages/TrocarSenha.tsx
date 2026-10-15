import { useNavigate } from 'react-router-dom'
import { FormularioTrocaSenha } from '../components/FormularioTrocaSenha'
import { useAuth } from '../context/AuthContext'
import { useTenantBranding } from '../context/TenantBrandingContext'
import logoSimbolo from '../assets/brand/agenda-saude-simbolo.svg'

/**
 * Troca obrigatória de senha no primeiro acesso (senha provisória definida por
 * um Admin). Fica fora do AdminLayout de propósito: o backend só libera a troca
 * de senha enquanto a senha for provisória, então o menu levaria a telas bloqueadas.
 */
export function TrocarSenha() {
  const { usuario, logout } = useAuth()
  const branding = useTenantBranding()
  const navigate = useNavigate()

  return (
    <div
      className="min-h-screen flex items-center justify-center px-4"
      
    >
      <div className="w-full max-w-sm bg-white/75 backdrop-blur-xl rounded-[2rem] p-8 animate-fade-in" style={{ boxShadow: 'var(--shadow-2xl)' }}>
        <div className="flex items-center gap-2 mb-6">
          {branding.logoUrl ? (
            <img src={branding.logoUrl} alt={branding.nomeMunicipio ?? 'Logo da Secretaria'} className="h-8 max-w-[160px] object-contain" />
          ) : (
            <img src={logoSimbolo} alt="Agenda Saúde" className="h-8 w-8" />
          )}
          <span className="font-bold text-lg text-brand-navy">Agenda Saúde</span>
        </div>

        <h1 className="text-xl font-bold text-gray-900 mb-1">Defina sua senha</h1>
        <p className="text-sm text-gray-500 mb-6">
          {usuario?.trocarSenha
            ? 'Você entrou com uma senha provisória. Escolha uma senha pessoal para continuar.'
            : 'Escolha uma nova senha para a sua conta.'}
        </p>

        <FormularioTrocaSenha
          aoConcluir={() => navigate(usuario?.papel === 'ACS' ? '/admin/meus-pacientes' : '/admin', { replace: true })}
        />

        <button
          type="button"
          onClick={() => {
            logout()
            navigate('/admin/login', { replace: true })
          }}
          className="mt-4 w-full text-center text-xs text-gray-500 hover:text-gray-700"
        >
          Sair
        </button>
      </div>
    </div>
  )
}
