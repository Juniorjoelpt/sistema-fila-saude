import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSuperadminAuth } from '../../context/SuperadminAuthContext'

export function SuperadminLogin() {
  const { login } = useSuperadminAuth()
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
      navigate('/superadmin')
    } catch {
      setErro('E-mail ou senha incorretos.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 to-gray-800 px-4">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow-2xl p-8 animate-fade-in">
        <div className="flex items-center gap-2 mb-6">
          <div className="h-8 w-8 rounded-lg bg-gray-900" />
          <span className="font-bold text-lg text-gray-900">MS Soluções</span>
        </div>
        <h1 className="text-xl font-bold text-gray-900 mb-1">Painel de Superadmin</h1>
        <p className="text-sm text-gray-500 mb-6">Gestão das prefeituras clientes do Fila Saúde.</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">E-mail</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-900"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Senha</label>
            <input
              type="password"
              required
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-900"
            />
          </div>

          {erro && <p className="text-sm text-red-600">{erro}</p>}

          <button
            type="submit"
            disabled={carregando}
            className="w-full flex items-center justify-center gap-2 rounded-lg bg-gray-900 text-white py-2.5 text-sm font-semibold hover:bg-black disabled:opacity-50"
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
