import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTenantBranding } from '../context/TenantBrandingContext'

export function Login() {
  const { login } = useAuth()
  const branding = useTenantBranding()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [carregando, setCarregando] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setCarregando(true)
    setErro(null)
    try {
      const papel = await login(email, senha)
      navigate(papel === 'ACS' ? '/admin/meus-pacientes' : '/admin')
    } catch {
      setErro('E-mail ou senha incorretos.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-brand-navy to-brand-navy-dark px-4">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow-2xl p-8 animate-fade-in">
        <div className="flex items-center gap-2 mb-6">
          {branding.logoUrl ? (
            <img
              src={branding.logoUrl}
              alt={branding.nomeMunicipio ?? 'Logo da Secretaria'}
              className="h-8 max-w-[160px] object-contain"
            />
          ) : (
            <div className="h-8 w-8 rounded-lg bg-brand-teal" />
          )}
          <span className="font-bold text-lg text-brand-navy">
            Fila Saúde{branding.nomeMunicipio ? ` · ${branding.nomeMunicipio}` : ''}
          </span>
        </div>
        <h1 className="text-xl font-bold text-gray-900 mb-1">Painel Administrativo</h1>
        <p className="text-sm text-gray-500 mb-6">Entre com suas credenciais de acesso.</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">E-mail</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-teal"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Senha</label>
            <input
              type="password"
              required
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-teal"
            />
          </div>

          {erro && <p className="text-sm text-red-600">{erro}</p>}

          <button
            type="submit"
            disabled={carregando}
            className="w-full flex items-center justify-center gap-2 rounded-lg bg-brand-navy text-white py-2.5 text-sm font-semibold hover:bg-brand-navy-dark disabled:opacity-50"
          >
            {carregando && (
              <span className="h-3.5 w-3.5 rounded-full border-2 border-white/40 border-t-white animate-spin" />
            )}
            {carregando ? 'Entrando…' : 'Entrar'}
          </button>
        </form>
      </div>
    </div>
  )
}
