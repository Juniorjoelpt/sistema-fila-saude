import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import type { Especialidade, Procedimento, SlaConfiguracao, SlaConfiguracoes as SlaConfiguracoesType } from '../api/types'

type Escopo = 'ESPECIALIDADE' | 'PROCEDIMENTO'

export function SlaConfiguracoes() {
  const [dados, setDados] = useState<SlaConfiguracoesType | null>(null)
  const [procedimentos, setProcedimentos] = useState<Procedimento[]>([])
  const [especialidades, setEspecialidades] = useState<Especialidade[]>([])
  const [carregando, setCarregando] = useState(false)
  const [erroCarregar, setErroCarregar] = useState<string | null>(null)

  const [escopo, setEscopo] = useState<Escopo>('ESPECIALIDADE')
  const [especialidade, setEspecialidade] = useState('')
  const [procedimentoId, setProcedimentoId] = useState<number | ''>('')
  const [diasAtencao, setDiasAtencao] = useState('')
  const [diasAtrasado, setDiasAtrasado] = useState('')
  const [erroForm, setErroForm] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  function carregar() {
    setCarregando(true)
    setErroCarregar(null)
    api
      .get<SlaConfiguracoesType>('/api/sla-configuracoes')
      .then((res) => setDados(res.data))
      .catch(() => setErroCarregar('Não foi possível carregar as configurações de SLA.'))
      .finally(() => setCarregando(false))
  }

  useEffect(() => {
    carregar()
    api
      .get<Procedimento[]>('/api/procedimentos')
      .then((res) => setProcedimentos(res.data))
      .catch(() => {
        // Lista só alimenta os selects do formulário -- uma falha aqui não
        // impede ver/gerenciar as configurações já existentes.
      })
    api.get<Especialidade[]>('/api/especialidades').then((res) => setEspecialidades(res.data)).catch(() => {})
  }, [])

  function limparForm() {
    setEspecialidade('')
    setProcedimentoId('')
    setDiasAtencao('')
    setDiasAtrasado('')
  }

  async function criar(e: FormEvent) {
    e.preventDefault()
    setErroForm(null)

    const atencao = Number(diasAtencao)
    const atrasado = Number(diasAtrasado)
    if (!Number.isFinite(atencao) || atencao < 1 || !Number.isFinite(atrasado) || atrasado < 1) {
      setErroForm('Informe valores válidos para os dois limiares.')
      return
    }
    if (atrasado <= atencao) {
      setErroForm('O limiar "Atrasado" deve ser maior que o de "Atenção".')
      return
    }
    if (escopo === 'ESPECIALIDADE' && !especialidade.trim()) {
      setErroForm('Informe a especialidade.')
      return
    }
    if (escopo === 'PROCEDIMENTO' && !procedimentoId) {
      setErroForm('Selecione o procedimento.')
      return
    }

    setSalvando(true)
    try {
      await api.post('/api/sla-configuracoes', {
        especialidade: escopo === 'ESPECIALIDADE' ? especialidade.trim() : null,
        procedimentoId: escopo === 'PROCEDIMENTO' ? procedimentoId : null,
        diasAtencao: atencao,
        diasAtrasado: atrasado,
      })
      limparForm()
      carregar()
    } catch (err: any) {
      setErroForm(err?.response?.data?.mensagem ?? 'Não foi possível cadastrar a configuração de SLA.')
    } finally {
      setSalvando(false)
    }
  }

  async function excluir(config: SlaConfiguracao) {
    try {
      await api.delete(`/api/sla-configuracoes/${config.id}`)
      carregar()
    } catch {
      setErroCarregar('Não foi possível remover esta configuração de SLA.')
    }
  }

  return (
    <AdminLayout>
      <h1 className="text-2xl font-bold text-gray-900 mb-1">Configurações de SLA</h1>
      <p className="text-sm text-gray-500 mb-6">
        Defina, por especialidade ou por procedimento, a partir de quantos dias aguardando um protocolo passa a ser
        considerado "Atenção" e depois "Atrasado" na fila e no painel. O que não tiver uma configuração própria
        continua usando o padrão do município{' '}
        {dados && (
          <span className="font-semibold text-gray-700">
            (Atenção: {dados.diasAtencaoPadrao} dias · Atrasado: {dados.diasAtrasadoPadrao} dias)
          </span>
        )}
        .
      </p>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <form onSubmit={criar} className="rounded-2xl border bg-white shadow-sm p-5 space-y-3 h-fit">
          <h2 className="text-sm font-semibold text-gray-700">Nova configuração</h2>

          <div className="flex gap-2 text-xs">
            <button
              type="button"
              onClick={() => setEscopo('ESPECIALIDADE')}
              className={`flex-1 rounded-lg border px-3 py-2 font-semibold ${
                escopo === 'ESPECIALIDADE' ? 'border-brand-teal bg-brand-teal-light text-brand-navy' : 'border-gray-300 text-gray-500'
              }`}
            >
              Por especialidade
            </button>
            <button
              type="button"
              onClick={() => setEscopo('PROCEDIMENTO')}
              className={`flex-1 rounded-lg border px-3 py-2 font-semibold ${
                escopo === 'PROCEDIMENTO' ? 'border-brand-teal bg-brand-teal-light text-brand-navy' : 'border-gray-300 text-gray-500'
              }`}
            >
              Por procedimento
            </button>
          </div>

          {escopo === 'ESPECIALIDADE' ? (
            <select
              value={especialidade}
              onChange={(e) => setEspecialidade(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
            >
              <option value="">Especialidade *</option>
              {especialidades.map((e) => (
                <option key={e.id} value={e.nome}>
                  {e.nome}
                </option>
              ))}
            </select>
          ) : (
            <select
              value={procedimentoId}
              onChange={(e) => setProcedimentoId(e.target.value ? Number(e.target.value) : '')}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
            >
              <option value="">Procedimento *</option>
              {procedimentos.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nome} {p.especialidade ? `(${p.especialidade})` : ''}
                </option>
              ))}
            </select>
          )}

          <div className="flex gap-2">
            <div className="flex-1">
              <label className="block text-xs text-gray-400 mb-1">Dias p/ "Atenção"</label>
              <input
                type="number"
                min={1}
                value={diasAtencao}
                onChange={(e) => setDiasAtencao(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="flex-1">
              <label className="block text-xs text-gray-400 mb-1">Dias p/ "Atrasado"</label>
              <input
                type="number"
                min={1}
                value={diasAtrasado}
                onChange={(e) => setDiasAtrasado(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
            </div>
          </div>

          {erroForm && <p className="text-xs text-red-600">{erroForm}</p>}
          <button
            type="submit"
            disabled={salvando}
            className="w-full btn-primary rounded-lg text-white py-2 text-sm font-semibold disabled:opacity-40"
          >
            {salvando ? 'Salvando…' : 'Adicionar configuração'}
          </button>
        </form>

        <div className="lg:col-span-2 space-y-3">
          {carregando && <p className="text-sm text-gray-400">Carregando…</p>}
          {erroCarregar && (
            <div className="mb-3 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between gap-3">
              <span>{erroCarregar}</span>
              <button onClick={carregar} className="font-semibold underline shrink-0">
                Tentar novamente
              </button>
            </div>
          )}
          {!carregando && dados && dados.configuracoes.length === 0 && (
            <p className="text-sm text-gray-400">
              Nenhuma configuração própria cadastrada ainda -- todas as especialidades/procedimentos usam o padrão do
              município.
            </p>
          )}
          {!carregando &&
            dados?.configuracoes.map((config) => (
              <div
                key={config.id}
                className="rounded-2xl border bg-white shadow-sm p-5 flex items-center justify-between gap-3"
              >
                <div>
                  <p className="font-semibold text-gray-900">
                    {config.procedimentoId ? config.nomeProcedimento : config.especialidade}
                  </p>
                  <p className="text-xs text-gray-400">
                    {config.procedimentoId ? 'Procedimento' : 'Especialidade'} · Atenção: {config.diasAtencao} dias ·
                    Atrasado: {config.diasAtrasado} dias
                  </p>
                </div>
                <button onClick={() => excluir(config)} className="text-xs text-red-600 hover:underline shrink-0">
                  Remover
                </button>
              </div>
            ))}
        </div>
      </div>
    </AdminLayout>
  )
}
