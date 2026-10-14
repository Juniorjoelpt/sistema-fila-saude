import { useState, type FormEvent } from 'react'
import { useAuth } from '../context/AuthContext'

/**
 * Troca da própria senha. Usado na tela obrigatória de primeiro acesso
 * (senha provisória) e na tela Segurança. As regras espelham o backend
 * (ContaService): mínimo de 8 caracteres, com letras e números, diferente da atual.
 */
export function FormularioTrocaSenha({ aoConcluir }: { aoConcluir?: () => void }) {
  const { alterarSenha } = useAuth()
  const [senhaAtual, setSenhaAtual] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [sucesso, setSucesso] = useState(false)

  async function enviar(event: FormEvent) {
    event.preventDefault()
    setErro(null)
    setSucesso(false)

    if (novaSenha.length < 8) {
      setErro('A nova senha deve ter ao menos 8 caracteres.')
      return
    }
    if (!/[A-Za-zÀ-ÿ]/.test(novaSenha) || !/\d/.test(novaSenha)) {
      setErro('A nova senha deve ter letras e números.')
      return
    }
    if (novaSenha !== confirmacao) {
      setErro('A confirmação não é igual à nova senha.')
      return
    }
    if (novaSenha === senhaAtual) {
      setErro('A nova senha deve ser diferente da senha atual.')
      return
    }

    setEnviando(true)
    try {
      await alterarSenha(senhaAtual, novaSenha)
      setSenhaAtual('')
      setNovaSenha('')
      setConfirmacao('')
      setSucesso(true)
      aoConcluir?.()
    } catch (err: any) {
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível alterar a senha.')
    } finally {
      setEnviando(false)
    }
  }

  const campo =
    'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-teal'

  return (
    <form onSubmit={enviar} className="space-y-3">
      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1" htmlFor="senha-atual">
          Senha atual (ou provisória)
        </label>
        <input
          id="senha-atual"
          type="password"
          autoComplete="current-password"
          required
          value={senhaAtual}
          onChange={(e) => setSenhaAtual(e.target.value)}
          className={campo}
        />
      </div>
      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1" htmlFor="nova-senha">
          Nova senha
        </label>
        <input
          id="nova-senha"
          type="password"
          autoComplete="new-password"
          required
          value={novaSenha}
          onChange={(e) => setNovaSenha(e.target.value)}
          className={campo}
        />
        <p className="mt-1 text-[11px] text-gray-400">Mínimo de 8 caracteres, com letras e números.</p>
      </div>
      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1" htmlFor="confirmar-senha">
          Confirmar nova senha
        </label>
        <input
          id="confirmar-senha"
          type="password"
          autoComplete="new-password"
          required
          value={confirmacao}
          onChange={(e) => setConfirmacao(e.target.value)}
          className={campo}
        />
      </div>
      {erro && <p className="text-xs text-red-600">{erro}</p>}
      {sucesso && <p className="text-xs text-brand-teal font-medium">Senha alterada com sucesso.</p>}
      <button
        type="submit"
        disabled={enviando}
        className="w-full btn-primary rounded-lg text-white py-2 text-sm font-semibold disabled:opacity-50"
      >
        {enviando ? 'Salvando…' : 'Alterar senha'}
      </button>
    </form>
  )
}
