import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function Login() {
  const { login } = useAuth()
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
      await login(email, senha)
      navigate('/admin')
    } catch {
      setErro('E-mail ou senha incorretos.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-brand-navy px-4">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow-xl p-8">
        <div className="flex items-center gap-2 mb-6">
          <div className="h-8 w-8 rounded-lg bg-brand-teal" />
          <span className="font-bold text-lg text-brand-navy">Fila Saúde</span>
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
            className="w-full rounded-lg bg-brand-navy text-white py-2.5 text-sm font-semibold hover:bg-brand-navy-dark disabled:opacity-50 transition-colors"
          >
            {carregando ? 'Entrando…' : 'Entrar'}
          </button>
        </form>
      </div>
    </div>
  )
}
