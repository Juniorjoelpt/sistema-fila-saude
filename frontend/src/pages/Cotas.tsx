import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import type { Cota, CotaAjuste, UnidadeSaude } from '../api/types'

function mesAtual(): string {
  const hoje = new Date()
  return `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}`
}

function corBarra(percentual: number): string {
  if (percentual >= 100) return 'bg-red-500'
  if (percentual >= 80) return 'bg-amber-500'
  return 'bg-brand-teal'
}

function LinhaCota({ cota, onAtualizado }: { cota: Cota; onAtualizado: () => void }) {
  const [expandido, setExpandido] = useState(false)
  const [novaQuantidade, setNovaQuantidade] = useState(String(cota.quantidadeTotal))
  const [motivo, setMotivo] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [historico, setHistorico] = useState<CotaAjuste[] | null>(null)

  async function ajustar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    const quantidade = Number(novaQuantidade)
    if (!Number.isFinite(quantidade) || quantidade < 0) {
      setErro('Informe uma quantidade válida.')
      return
    }
    setSalvando(true)
    try {
      await api.patch(`/api/cotas/${cota.id}`, { quantidadeTotal: quantidade, motivo: motivo || null })
      setMotivo('')
      setExpandido(false)
      onAtualizado()
    } catch {
      setErro('Não foi possível ajustar a cota.')
    } finally {
      setSalvando(false)
    }
  }

  async function verHistorico() {
    if (historico) {
      setHistorico(null)
      return
    }
    const { data } = await api.get<CotaAjuste[]>(`/api/cotas/${cota.id}/ajustes`)
    setHistorico(data)
  }

  return (
    <div className="rounded-2xl border bg-white shadow-sm p-5">
      <div className="flex flex-wrap items-start justify-between gap-3 mb-3">
        <div>
          <p className="font-semibold text-gray-900">{cota.especialidade}</p>
          <p className="text-xs text-gray-400">{cota.nomeUnidadeSaude}</p>
        </div>
        <div className="text-right">
          <p className="text-sm font-semibold text-gray-900">
            {cota.quantidadeUtilizada} / {cota.quantidadeTotal}
          </p>
          <p className="text-xs text-gray-400">{cota.percentualPreenchido}% preenchido</p>
        </div>
      </div>

      <div className="h-2 rounded-full bg-gray-100 overflow-hidden mb-3">
        <div
          className={`h-full ${corBarra(cota.percentualPreenchido)}`}
          style={{ width: `${Math.min(cota.percentualPreenchido, 100)}%` }}
        />
      </div>

      <div className="flex gap-3 text-xs">
        <button onClick={() => setExpandido((v) => !v)} className="text-brand-navy font-semibold hover:underline">
          {expandido ? 'Cancelar ajuste' : 'Ajustar cota'}
        </button>
        <button onClick={verHistorico} className="text-gray-500 hover:underline">
          {historico ? 'Ocultar histórico' : 'Ver histórico'}
        </button>
      </div>

      {expandido && (
        <form onSubmit={ajustar} className="mt-4 flex flex-wrap items-end gap-2 border-t pt-4">
          <div>
            <label className="block text-xs text-gray-400 mb-1">Nova quantidade</label>
            <input
              type="number"
              min={0}
              value={novaQuantidade}
              onChange={(e) => setNovaQuantidade(e.target.value)}
              className="w-28 rounded-lg border border-gray-300 px-3 py-1.5 text-sm"
            />
          </div>
          <div className="flex-1 min-w-[160px]">
            <label className="block text-xs text-gray-400 mb-1">Motivo (opcional)</label>
            <input
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              placeholder="Ex.: aumento sazonal de demanda"
              className="w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm"
            />
          </div>
          <button
            type="submit"
            disabled={salvando}
            className="rounded-lg bg-brand-navy text-white px-4 py-1.5 text-sm font-semibold disabled:opacity-40"
          >
            {salvando ? 'Salvando…' : 'Salvar'}
          </button>
          {erro && <p className="w-full text-xs text-red-600">{erro}</p>}
        </form>
      )}

      {historico && (
        <div className="mt-4 border-t pt-4 space-y-2">
          {historico.length === 0 && <p className="text-xs text-gray-400">Nenhum ajuste registrado ainda.</p>}
          {historico.map((h, idx) => (
            <div key={idx} className="text-xs text-gray-600">
              <span className="font-semibold text-gray-800">
                {h.quantidadeAnterior} → {h.quantidadeNova}
              </span>{' '}
              por {h.usuarioNome} em {new Date(h.criadoEm).toLocaleString('pt-BR')}
              {h.motivo && <span className="text-gray-400"> — {h.motivo}</span>}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export function Cotas() {
  const [mes, setMes] = useState(mesAtual())
  const [unidadeSaudeId, setUnidadeSaudeId] = useState<number | ''>('')
  const [unidades, setUnidades] = useState<UnidadeSaude[]>([])
  const [cotas, setCotas] = useState<Cota[]>([])
  const [carregando, setCarregando] = useState(false)

  const [novaUnidade, setNovaUnidade] = useState<number | ''>('')
  const [novaEspecialidade, setNovaEspecialidade] = useState('')
  const [novaQuantidade, setNovaQuantidade] = useState('')
  const [erroForm, setErroForm] = useState<string | null>(null)
  const [salvandoForm, setSalvandoForm] = useState(false)

  useEffect(() => {
    api.get<UnidadeSaude[]>('/api/unidades').then((res) => setUnidades(res.data))
  }, [])

  function carregar() {
    setCarregando(true)
    api
      .get<Cota[]>('/api/cotas', { params: { mesReferencia: mes, unidadeSaudeId: unidadeSaudeId || undefined } })
      .then((res) => setCotas(res.data))
      .finally(() => setCarregando(false))
  }

  useEffect(carregar, [mes, unidadeSaudeId])

  async function criarCota(e: FormEvent) {
    e.preventDefault()
    setErroForm(null)
    const quantidade = Number(novaQuantidade)
    if (!novaUnidade || !novaEspecialidade.trim() || !Number.isFinite(quantidade) || quantidade < 0) {
      setErroForm('Preencha unidade, especialidade e uma quantidade válida.')
      return
    }
    setSalvandoForm(true)
    try {
      await api.post('/api/cotas', {
        unidadeSaudeId: novaUnidade,
        especialidade: novaEspecialidade,
        mesReferencia: mes,
        quantidadeTotal: quantidade,
      })
      setNovaEspecialidade('')
      setNovaQuantidade('')
      carregar()
    } catch (err: any) {
      setErroForm(err?.response?.data?.mensagem ?? 'Não foi possível cadastrar a cota.')
    } finally {
      setSalvandoForm(false)
    }
  }

  return (
    <AdminLayout>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Gestão de Cotas</h1>

      <div className="flex flex-wrap gap-3 mb-6">
        <input
          type="month"
          value={mes}
          onChange={(e) => setMes(e.target.value)}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        <select
          value={unidadeSaudeId}
          onChange={(e) => setUnidadeSaudeId(e.target.value ? Number(e.target.value) : '')}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        >
          <option value="">Todas as unidades</option>
          {unidades.map((u) => (
            <option key={u.id} value={u.id}>
              {u.nome}
            </option>
          ))}
        </select>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <form onSubmit={criarCota} className="rounded-2xl border bg-white shadow-sm p-5 space-y-3 h-fit">
          <h2 className="text-sm font-semibold text-gray-700">Nova cota para {mes}</h2>
          <select
            value={novaUnidade}
            onChange={(e) => setNovaUnidade(e.target.value ? Number(e.target.value) : '')}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
          >
            <option value="">Unidade de saúde *</option>
            {unidades.map((u) => (
              <option key={u.id} value={u.id}>
                {u.nome}
              </option>
            ))}
          </select>
          <input
            placeholder="Especialidade *"
            value={novaEspecialidade}
            onChange={(e) => setNovaEspecialidade(e.target.value)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
          />
          <input
            type="number"
            min={0}
            placeholder="Quantidade de vagas *"
            value={novaQuantidade}
            onChange={(e) => setNovaQuantidade(e.target.value)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
          />
          {erroForm && <p className="text-xs text-red-600">{erroForm}</p>}
          <button
            type="submit"
            disabled={salvandoForm}
            className="w-full rounded-lg bg-brand-navy text-white py-2 text-sm font-semibold disabled:opacity-40"
          >
            {salvandoForm ? 'Salvando…' : 'Adicionar cota'}
          </button>
        </form>

        <div className="lg:col-span-2 space-y-4">
          {carregando && <p className="text-sm text-gray-400">Carregando…</p>}
          {!carregando && cotas.length === 0 && (
            <p className="text-sm text-gray-400">Nenhuma cota cadastrada para {mes}.</p>
          )}
          {!carregando && cotas.map((cota) => <LinhaCota key={cota.id} cota={cota} onAtualizado={carregar} />)}
        </div>
      </div>
    </AdminLayout>
  )
}
