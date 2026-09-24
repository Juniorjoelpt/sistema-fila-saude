import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import type { HorarioAgenda, UnidadeSaude } from '../api/types'

const DIAS_SEMANA: { valor: string; rotulo: string }[] = [
  { valor: 'MONDAY', rotulo: 'Seg' },
  { valor: 'TUESDAY', rotulo: 'Ter' },
  { valor: 'WEDNESDAY', rotulo: 'Qua' },
  { valor: 'THURSDAY', rotulo: 'Qui' },
  { valor: 'FRIDAY', rotulo: 'Sex' },
  { valor: 'SATURDAY', rotulo: 'Sáb' },
  { valor: 'SUNDAY', rotulo: 'Dom' },
]

function hojeISO(): string {
  return new Date().toISOString().slice(0, 10)
}

function corVagas(disponiveis: number, total: number): string {
  if (disponiveis === 0) return 'text-red-600'
  if (disponiveis <= total * 0.3) return 'text-amber-600'
  return 'text-brand-teal'
}

export function Agenda() {
  const [unidades, setUnidades] = useState<UnidadeSaude[]>([])
  const [unidadeSaudeId, setUnidadeSaudeId] = useState<number | ''>('')
  const [especialidadeFiltro, setEspecialidadeFiltro] = useState('')
  const [dataInicio, setDataInicio] = useState(hojeISO())
  const [dataFim, setDataFim] = useState('')
  const [horarios, setHorarios] = useState<HorarioAgenda[]>([])
  const [carregando, setCarregando] = useState(false)
  const [mensagem, setMensagem] = useState<string | null>(null)

  useEffect(() => {
    api.get<UnidadeSaude[]>('/api/unidades').then((res) => setUnidades(res.data))
  }, [])

  function carregar() {
    if (!unidadeSaudeId) {
      setHorarios([])
      return
    }
    setCarregando(true)
    api
      .get<HorarioAgenda[]>('/api/horarios-agenda', {
        params: {
          unidadeSaudeId,
          especialidade: especialidadeFiltro || undefined,
          dataInicio: dataInicio || undefined,
          dataFim: dataFim || undefined,
        },
      })
      .then((res) => setHorarios(res.data))
      .finally(() => setCarregando(false))
  }

  useEffect(carregar, [unidadeSaudeId, especialidadeFiltro, dataInicio, dataFim])

  async function remover(id: number) {
    setMensagem(null)
    try {
      await api.delete(`/api/horarios-agenda/${id}`)
      carregar()
    } catch (err: any) {
      setMensagem(err?.response?.data?.mensagem ?? 'Não foi possível remover este horário.')
    }
  }

  // Formulário: horário único
  const [novaUnidade, setNovaUnidade] = useState<number | ''>('')
  const [novaEspecialidade, setNovaEspecialidade] = useState('')
  const [novaData, setNovaData] = useState('')
  const [novaHora, setNovaHora] = useState('')
  const [novaCapacidade, setNovaCapacidade] = useState('1')
  const [erroUnico, setErroUnico] = useState<string | null>(null)
  const [salvandoUnico, setSalvandoUnico] = useState(false)

  async function criarUnico(e: FormEvent) {
    e.preventDefault()
    setErroUnico(null)
    const capacidade = Number(novaCapacidade)
    if (!novaUnidade || !novaEspecialidade.trim() || !novaData || !novaHora || !Number.isFinite(capacidade) || capacidade < 1) {
      setErroUnico('Preencha unidade, especialidade, data, hora e uma capacidade válida (mínimo 1).')
      return
    }
    setSalvandoUnico(true)
    try {
      await api.post('/api/horarios-agenda', {
        unidadeSaudeId: novaUnidade,
        especialidade: novaEspecialidade,
        data: novaData,
        horaInicio: novaHora,
        capacidadeTotal: capacidade,
      })
      setNovaData('')
      setNovaHora('')
      setNovaCapacidade('1')
      carregar()
    } catch (err: any) {
      setErroUnico(err?.response?.data?.mensagem ?? 'Não foi possível cadastrar o horário.')
    } finally {
      setSalvandoUnico(false)
    }
  }

  // Formulário: geração em lote (agenda recorrente)
  const [loteUnidade, setLoteUnidade] = useState<number | ''>('')
  const [loteEspecialidade, setLoteEspecialidade] = useState('')
  const [loteDataInicio, setLoteDataInicio] = useState('')
  const [loteDataFim, setLoteDataFim] = useState('')
  const [loteDias, setLoteDias] = useState<Set<string>>(new Set())
  const [loteHoraInicio, setLoteHoraInicio] = useState('')
  const [loteHoraFim, setLoteHoraFim] = useState('')
  const [loteIntervalo, setLoteIntervalo] = useState('20')
  const [loteCapacidade, setLoteCapacidade] = useState('1')
  const [erroLote, setErroLote] = useState<string | null>(null)
  const [salvandoLote, setSalvandoLote] = useState(false)

  function alternarDia(dia: string) {
    setLoteDias((atual) => {
      const novo = new Set(atual)
      if (novo.has(dia)) novo.delete(dia)
      else novo.add(dia)
      return novo
    })
  }

  async function gerarLote(e: FormEvent) {
    e.preventDefault()
    setErroLote(null)
    const intervalo = Number(loteIntervalo)
    const capacidade = Number(loteCapacidade)
    if (
      !loteUnidade ||
      !loteEspecialidade.trim() ||
      !loteDataInicio ||
      !loteDataFim ||
      loteDias.size === 0 ||
      !loteHoraInicio ||
      !loteHoraFim ||
      !Number.isFinite(intervalo) ||
      intervalo < 5 ||
      !Number.isFinite(capacidade) ||
      capacidade < 1
    ) {
      setErroLote('Preencha todos os campos: período, dias da semana, faixa de horário, intervalo (mín. 5min) e capacidade (mín. 1).')
      return
    }
    setSalvandoLote(true)
    try {
      const res = await api.post<{ criados: number }>('/api/horarios-agenda/lote', {
        unidadeSaudeId: loteUnidade,
        especialidade: loteEspecialidade,
        dataInicio: loteDataInicio,
        dataFim: loteDataFim,
        diasSemana: Array.from(loteDias),
        horaInicio: loteHoraInicio,
        horaFim: loteHoraFim,
        intervaloMinutos: intervalo,
        capacidadePorHorario: capacidade,
      })
      setMensagem(`${res.data.criados} horário(s) gerado(s) com sucesso.`)
      carregar()
    } catch (err: any) {
      setErroLote(err?.response?.data?.mensagem ?? 'Não foi possível gerar os horários.')
    } finally {
      setSalvandoLote(false)
    }
  }

  return (
    <AdminLayout>
      <h1 className="text-2xl font-bold text-gray-900 mb-1">Agenda de Horários</h1>
      <p className="text-sm text-gray-500 mb-6">
        Cadastre horários reais (unidade + especialidade + data + hora, com vagas) para agendar protocolos com dia e
        hora marcados — em vez de só uma data solta.
      </p>

      {mensagem && <p className="text-sm text-brand-teal font-medium mb-4">{mensagem}</p>}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        <form onSubmit={criarUnico} className="rounded-2xl border bg-white shadow-sm p-5 space-y-3 h-fit">
          <h2 className="text-sm font-semibold text-gray-700">Novo horário</h2>
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
          <div className="flex gap-2">
            <input
              type="date"
              value={novaData}
              onChange={(e) => setNovaData(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
            <input
              type="time"
              value={novaHora}
              onChange={(e) => setNovaHora(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
          </div>
          <input
            type="number"
            min={1}
            placeholder="Vagas *"
            value={novaCapacidade}
            onChange={(e) => setNovaCapacidade(e.target.value)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
          />
          {erroUnico && <p className="text-xs text-red-600">{erroUnico}</p>}
          <button
            type="submit"
            disabled={salvandoUnico}
            className="w-full rounded-lg bg-brand-navy text-white py-2 text-sm font-semibold disabled:opacity-40"
          >
            {salvandoUnico ? 'Salvando…' : 'Adicionar horário'}
          </button>
        </form>

        <form onSubmit={gerarLote} className="lg:col-span-2 rounded-2xl border bg-white shadow-sm p-5 space-y-3 h-fit">
          <h2 className="text-sm font-semibold text-gray-700">Gerar agenda recorrente</h2>
          <p className="text-xs text-gray-400 -mt-2">
            Cria um horário para cada dia da semana selecionado, dentro do período, do início ao fim, em intervalos
            fixos — útil para uma unidade que atende, por exemplo, toda segunda e quarta das 8h às 12h de 20 em 20
            minutos.
          </p>
          <div className="grid grid-cols-2 gap-2">
            <select
              value={loteUnidade}
              onChange={(e) => setLoteUnidade(e.target.value ? Number(e.target.value) : '')}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
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
              value={loteEspecialidade}
              onChange={(e) => setLoteEspecialidade(e.target.value)}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-xs text-gray-400 mb-1">Data inicial</label>
              <input
                type="date"
                value={loteDataInicio}
                onChange={(e) => setLoteDataInicio(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-1">Data final</label>
              <input
                type="date"
                value={loteDataFim}
                onChange={(e) => setLoteDataFim(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs text-gray-400 mb-1">Dias da semana</label>
            <div className="flex flex-wrap gap-1.5">
              {DIAS_SEMANA.map((d) => (
                <button
                  key={d.valor}
                  type="button"
                  onClick={() => alternarDia(d.valor)}
                  className={`rounded-lg border px-2.5 py-1 text-xs font-semibold ${
                    loteDias.has(d.valor)
                      ? 'bg-brand-navy text-white border-brand-navy'
                      : 'border-gray-300 text-gray-600 hover:bg-gray-50'
                  }`}
                >
                  {d.rotulo}
                </button>
              ))}
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-xs text-gray-400 mb-1">Hora inicial</label>
              <input
                type="time"
                value={loteHoraInicio}
                onChange={(e) => setLoteHoraInicio(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-1">Hora final</label>
              <input
                type="time"
                value={loteHoraFim}
                onChange={(e) => setLoteHoraFim(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-xs text-gray-400 mb-1">Intervalo (min)</label>
              <input
                type="number"
                min={5}
                value={loteIntervalo}
                onChange={(e) => setLoteIntervalo(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-1">Vagas por horário</label>
              <input
                type="number"
                min={1}
                value={loteCapacidade}
                onChange={(e) => setLoteCapacidade(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
            </div>
          </div>
          {erroLote && <p className="text-xs text-red-600">{erroLote}</p>}
          <button
            type="submit"
            disabled={salvandoLote}
            className="w-full rounded-lg bg-brand-teal text-white py-2 text-sm font-semibold disabled:opacity-40"
          >
            {salvandoLote ? 'Gerando…' : 'Gerar horários'}
          </button>
        </form>
      </div>

      <h2 className="text-sm font-semibold text-gray-700 mb-3">Horários cadastrados</h2>
      <div className="flex flex-wrap gap-3 mb-4">
        <select
          value={unidadeSaudeId}
          onChange={(e) => setUnidadeSaudeId(e.target.value ? Number(e.target.value) : '')}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        >
          <option value="">Selecione uma unidade para ver a agenda</option>
          {unidades.map((u) => (
            <option key={u.id} value={u.id}>
              {u.nome}
            </option>
          ))}
        </select>
        <input
          placeholder="Filtrar por especialidade"
          value={especialidadeFiltro}
          onChange={(e) => setEspecialidadeFiltro(e.target.value)}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        <input
          type="date"
          value={dataInicio}
          onChange={(e) => setDataInicio(e.target.value)}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        <input
          type="date"
          value={dataFim}
          onChange={(e) => setDataFim(e.target.value)}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          placeholder="Data final (opcional)"
        />
      </div>

      {!unidadeSaudeId && <p className="text-sm text-gray-400">Selecione uma unidade de saúde para ver a agenda.</p>}
      {unidadeSaudeId && carregando && <p className="text-sm text-gray-400">Carregando…</p>}
      {unidadeSaudeId && !carregando && horarios.length === 0 && (
        <p className="text-sm text-gray-400">Nenhum horário cadastrado para os filtros selecionados.</p>
      )}

      {unidadeSaudeId && !carregando && horarios.length > 0 && (
        <div className="rounded-2xl border bg-white shadow-sm overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
              <tr>
                <th className="text-left px-4 py-3">Data</th>
                <th className="text-left px-4 py-3">Hora</th>
                <th className="text-left px-4 py-3">Especialidade</th>
                <th className="text-left px-4 py-3">Vagas</th>
                <th className="px-4 py-3 w-10"></th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {horarios.map((h) => (
                <tr key={h.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-900">{new Date(h.data + 'T00:00:00').toLocaleDateString('pt-BR')}</td>
                  <td className="px-4 py-3 text-gray-900 font-mono">{h.horaInicio.slice(0, 5)}</td>
                  <td className="px-4 py-3 text-gray-600">{h.especialidade}</td>
                  <td className={`px-4 py-3 font-semibold ${corVagas(h.vagasDisponiveis, h.capacidadeTotal)}`}>
                    {h.vagasOcupadas} / {h.capacidadeTotal} ocupadas
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => remover(h.id)}
                      disabled={h.vagasOcupadas > 0}
                      title={h.vagasOcupadas > 0 ? 'Não é possível remover: há protocolo(s) agendado(s) aqui' : 'Remover horário'}
                      className="text-xs text-red-600 hover:text-red-700 disabled:text-gray-300 disabled:cursor-not-allowed"
                    >
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </AdminLayout>
  )
}
