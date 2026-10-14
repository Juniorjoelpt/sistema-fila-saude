import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import { CategoriaBadge, StatusBadge } from '../components/StatusBadge'
import { Spinner } from '../components/Spinner'
import { useAuth } from '../context/AuthContext'
import type {
  CategoriaPrioridade,
  HorarioAgenda,
  ProtocoloDetalhe as ProtocoloDetalheType,
  StatusEtapa,
  StatusProtocolo,
  UnidadeSaude,
} from '../api/types'

function hojeISO(): string {
  return new Date().toISOString().slice(0, 10)
}

const ROTULO_STATUS: Record<StatusProtocolo, string> = {
  AGUARDANDO: 'Aguardando',
  AGENDADO: 'Agendado',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
}

// Espelha a máquina de estados do backend (ProtocoloService#TRANSICOES_PERMITIDAS),
// sem "Agendado" como destino: agendar tem caminho próprio ("Agendar horário" ou
// "Distribuir vagas"), que grava data, hora, unidade e vaga. Assim o seletor só
// oferece o que o backend aceita, em vez de recusar depois de salvar.
const TRANSICOES_MANUAIS: Record<StatusProtocolo, StatusProtocolo[]> = {
  AGUARDANDO: ['CANCELADO'],
  AGENDADO: ['EM_ANDAMENTO', 'AGUARDANDO', 'CANCELADO'],
  EM_ANDAMENTO: ['CONCLUIDO', 'CANCELADO'],
  CONCLUIDO: [],
  CANCELADO: [],
}

const CATEGORIAS_PRIORIDADE: { valor: CategoriaPrioridade; rotulo: string }[] = [
  { valor: 'URGENCIA', rotulo: 'Urgência' },
  { valor: 'JUDICIAL', rotulo: 'Judicial' },
  { valor: 'ESPECIAL', rotulo: 'Especial (80+)' },
  { valor: 'LEGAL', rotulo: 'Legal (60+, PCD, gestante)' },
  { valor: 'NORMAL', rotulo: 'Normal' },
]

const ESTILO_ETAPA: Record<StatusEtapa, { ponto: string; texto: string; tag: string; rotulo: string }> = {
  REALIZADO: { ponto: 'bg-brand-teal border-brand-teal', texto: 'text-gray-900', tag: 'bg-teal-100 text-teal-800', rotulo: 'Realizado' },
  EM_ANDAMENTO: { ponto: 'bg-brand-navy border-brand-navy', texto: 'text-gray-900 font-semibold', tag: 'bg-blue-100 text-blue-800', rotulo: 'Em andamento' },
  AGUARDANDO: { ponto: 'bg-white border-gray-300', texto: 'text-gray-400', tag: 'bg-gray-100 text-gray-500', rotulo: 'Aguardando' },
}

export function ProtocoloDetalhe() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { usuario } = useAuth()
  const podeRegular = usuario?.papel !== 'ACS'
  const [protocolo, setProtocolo] = useState<ProtocoloDetalheType | null>(null)
  const encerrado = protocolo?.status === 'CONCLUIDO' || protocolo?.status === 'CANCELADO'
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [novoStatus, setNovoStatus] = useState<StatusProtocolo>('AGUARDANDO')
  const [observacao, setObservacao] = useState('')
  const [salvandoStatus, setSalvandoStatus] = useState(false)
  const [novaCategoria, setNovaCategoria] = useState<CategoriaPrioridade>('NORMAL')
  const [processoJudicial, setProcessoJudicial] = useState('')
  const [motivoPrioridade, setMotivoPrioridade] = useState('')
  const [salvandoPrioridade, setSalvandoPrioridade] = useState(false)

  // Agendamento de horário real (unidade + especialidade + data + hora, com vaga).
  const [unidades, setUnidades] = useState<UnidadeSaude[]>([])
  const [mostrarAgendamento, setMostrarAgendamento] = useState(false)
  const [unidadeAgendamento, setUnidadeAgendamento] = useState<number | ''>('')
  const [dataAgenda, setDataAgenda] = useState(hojeISO())
  const [horariosDisponiveis, setHorariosDisponiveis] = useState<HorarioAgenda[]>([])
  const [carregandoHorarios, setCarregandoHorarios] = useState(false)
  const [horarioSelecionado, setHorarioSelecionado] = useState<number | ''>('')
  const [agendando, setAgendando] = useState(false)
  const [erroAgendamento, setErroAgendamento] = useState<string | null>(null)

  function carregar() {
    setCarregando(true)
    api
      .get<ProtocoloDetalheType>(`/api/fila/${id}`)
      .then((res) => {
        setProtocolo(res.data)
        setNovoStatus(res.data.status)
        setNovaCategoria(res.data.categoriaPrioridade)
        setProcessoJudicial(res.data.processoJudicial ?? '')
        setUnidadeAgendamento(res.data.unidadeSaudeId ?? '')
      })
      .catch(() => setErro('Não foi possível carregar o protocolo.'))
      .finally(() => setCarregando(false))
  }

  useEffect(() => {
    carregar()
    api.get<UnidadeSaude[]>('/api/unidades').then((res) => setUnidades(res.data))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  const controllerHorariosRef = useRef<AbortController | null>(null)

  useEffect(() => {
    controllerHorariosRef.current?.abort()
    if (!mostrarAgendamento || !unidadeAgendamento || !protocolo?.especialidadeProcedimento) {
      setHorariosDisponiveis([])
      return
    }
    // Bug alto de revisão corrigido: a busca de horários disponíveis não
    // tinha tratamento de erro (falha de rede deixava a lista vazia sem
    // avisar, parecendo "sem horário disponível") nem cancelamento -- trocar
    // rapidamente de unidade/data podia fazer uma resposta antiga chegar
    // depois e sobrescrever a busca mais recente.
    const controller = new AbortController()
    controllerHorariosRef.current = controller
    setCarregandoHorarios(true)
    setErroAgendamento(null)
    api
      .get<HorarioAgenda[]>('/api/horarios-agenda', {
        params: {
          unidadeSaudeId: unidadeAgendamento,
          especialidade: protocolo.especialidadeProcedimento,
          dataInicio: dataAgenda || hojeISO(),
        },
        signal: controller.signal,
      })
      .then((res) => setHorariosDisponiveis(res.data))
      .catch((err) => {
        if (err?.code === 'ERR_CANCELED') return
        setHorariosDisponiveis([])
        setErroAgendamento('Não foi possível carregar os horários disponíveis. Tente novamente.')
      })
      .finally(() => {
        if (controllerHorariosRef.current === controller) {
          setCarregandoHorarios(false)
        }
      })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mostrarAgendamento, unidadeAgendamento, dataAgenda, protocolo?.especialidadeProcedimento])

  async function confirmarAgendamento() {
    if (!horarioSelecionado) return
    setAgendando(true)
    setErroAgendamento(null)
    setErro(null)
    try {
      await api.post(`/api/fila/${id}/agendar-horario`, { horarioAgendaId: horarioSelecionado })
      setMostrarAgendamento(false)
      setHorarioSelecionado('')
      carregar()
    } catch (err: any) {
      setErroAgendamento(err?.response?.data?.mensagem ?? 'Não foi possível confirmar o agendamento.')
    } finally {
      setAgendando(false)
    }
  }

  async function alterarStatus() {
    if (novoStatus === 'CANCELADO' && !observacao.trim()) {
      setErro('Informe o motivo do cancelamento no campo de observação.')
      return
    }
    setSalvandoStatus(true)
    setErro(null)
    try {
      await api.patch(`/api/fila/${id}/status`, { novoStatus, observacao: observacao || null })
      setObservacao('')
      carregar()
    } catch (err: any) {
      // Agora o backend valida a transição de status (ex.: não permite
      // CONCLUIDO -> AGUARDANDO) -- mostrar a mensagem real ajuda o operador
      // a entender por que a mudança foi rejeitada.
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível alterar o status.')
    } finally {
      setSalvandoStatus(false)
    }
  }

  async function alterarPrioridade() {
    if (!motivoPrioridade.trim()) {
      setErro('Informe o motivo da reclassificação de prioridade.')
      return
    }
    if (novaCategoria === 'JUDICIAL' && !processoJudicial.trim()) {
      setErro('Informe o número do processo judicial para a categoria Judicial.')
      return
    }
    setSalvandoPrioridade(true)
    setErro(null)
    try {
      await api.patch(`/api/fila/${id}/prioridade`, {
        novaCategoria,
        processoJudicial: novaCategoria === 'JUDICIAL' ? processoJudicial : null,
        motivo: motivoPrioridade,
      })
      setMotivoPrioridade('')
      carregar()
    } catch (err: any) {
      // Agora o backend valida os critérios reais de Especial (80+) / Legal
      // (60+, PCD, gestante) -- mostrar a mensagem real explica a rejeição.
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível alterar a prioridade.')
    } finally {
      setSalvandoPrioridade(false)
    }
  }

  const [marcandoEtapaId, setMarcandoEtapaId] = useState<number | null>(null)

  async function marcarEtapa(etapaId: number, status: StatusEtapa) {
    // Bug médio de revisão corrigido: os botões de marcar etapa não tinham
    // proteção contra duplo clique -- dois cliques rápidos disparavam duas
    // requisições concorrentes para a mesma etapa.
    if (marcandoEtapaId !== null) return
    setMarcandoEtapaId(etapaId)
    setErro(null)
    try {
      await api.patch(`/api/fila/${id}/etapas/${etapaId}`, null, { params: { status } })
      carregar()
    } catch (err: any) {
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível atualizar a etapa.')
    } finally {
      setMarcandoEtapaId(null)
    }
  }

  if (carregando) {
    return (
      <AdminLayout>
        <div className="flex items-center gap-2 text-sm text-gray-400">
          <Spinner />
          Carregando…
        </div>
      </AdminLayout>
    )
  }

  if (!protocolo) {
    return (
      <AdminLayout>
        <p className="text-sm text-red-600">{erro ?? 'Protocolo não encontrado.'}</p>
      </AdminLayout>
    )
  }

  return (
    <AdminLayout>
      <button
        onClick={() => navigate(podeRegular ? '/admin/fila' : '/admin/meus-pacientes')}
        className="text-sm text-gray-500 hover:text-gray-700 mb-4"
      >
        {podeRegular ? '← Voltar para a fila' : '← Voltar para meus pacientes'}
      </button>

      <div className="flex flex-wrap items-start justify-between gap-3 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{protocolo.nomePaciente}</h1>
          <p className="text-sm text-gray-500 font-mono">{protocolo.numeroProtocolo}</p>
        </div>
        <div className="flex gap-2">
          <CategoriaBadge categoria={protocolo.categoriaPrioridade} />
          <StatusBadge status={protocolo.status} />
        </div>
      </div>

      {erro && <p className="text-sm text-red-600 mb-4">{erro}</p>}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <section className="rounded-2xl border bg-white shadow-sm p-6">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">Dados do protocolo</h2>
            <dl className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <dt className="text-xs text-gray-400 uppercase">Procedimento</dt>
                <dd className="text-gray-900">{protocolo.nomeProcedimento}</dd>
              </div>
              <div>
                <dt className="text-xs text-gray-400 uppercase">Unidade de saúde</dt>
                <dd className="text-gray-900">{protocolo.nomeUnidadeSaude ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-xs text-gray-400 uppercase">Data de solicitação</dt>
                <dd className="text-gray-900">{new Date(protocolo.dataSolicitacao + 'T00:00:00').toLocaleDateString('pt-BR')}</dd>
              </div>
              <div>
                <dt className="text-xs text-gray-400 uppercase">Dias em espera</dt>
                <dd className="text-gray-900">{protocolo.diasEmEspera} dias</dd>
              </div>
              {protocolo.status !== 'AGUARDANDO' && protocolo.dataPrevista && (
                <div>
                  <dt className="text-xs text-gray-400 uppercase">Data e hora agendada</dt>
                  <dd className="text-gray-900">
                    {new Date(protocolo.dataPrevista + 'T00:00:00').toLocaleDateString('pt-BR')}
                    {protocolo.horaAgendada ? ` às ${protocolo.horaAgendada.slice(0, 5)}` : ''}
                  </dd>
                </div>
              )}
              {protocolo.posicaoFila != null && (
                <div>
                  <dt className="text-xs text-gray-400 uppercase">Posição na fila</dt>
                  <dd className="text-gray-900">{protocolo.posicaoFila}º</dd>
                </div>
              )}
              {protocolo.processoJudicial && (
                <div>
                  <dt className="text-xs text-gray-400 uppercase">Processo judicial</dt>
                  <dd className="text-gray-900">{protocolo.processoJudicial}</dd>
                </div>
              )}
            </dl>
          </section>

          <section className="rounded-2xl border bg-white shadow-sm p-6">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">Linha do tempo</h2>
            {protocolo.etapas.length === 0 ? (
              <p className="text-sm text-gray-400">Nenhuma etapa cadastrada para este protocolo.</p>
            ) : (
              <ol className="relative border-l-2 border-gray-200 ml-3">
                {protocolo.etapas.map((etapa) => {
                  const estilo = ESTILO_ETAPA[etapa.status]
                  // Regras de ordem espelham o backend (ProtocoloService#marcarEtapa).
                  const anteriorPendente = protocolo.etapas.some((o) => o.ordem < etapa.ordem && o.status !== 'REALIZADO')
                  const posteriorIniciada = protocolo.etapas.some((o) => o.ordem > etapa.ordem && o.status !== 'AGUARDANDO')
                  const outraEmAndamento = protocolo.etapas.some((o) => o.id !== etapa.id && o.status === 'EM_ANDAMENTO')
                  return (
                    <li key={etapa.id} className="mb-6 ml-6 last:mb-0">
                      <span className={`absolute -left-[9px] flex h-4 w-4 items-center justify-center rounded-full border-2 ${estilo.ponto}`} />
                      <div className="flex flex-wrap items-center gap-2 mb-1">
                        <p className={`text-sm ${estilo.texto}`}>{etapa.nomeEtapa}</p>
                        <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${estilo.tag}`}>
                          {estilo.rotulo}
                        </span>
                      </div>
                      {etapa.dataRealizacao && (
                        <p className="text-xs text-gray-400 mb-2">
                          {new Date(etapa.dataRealizacao + 'T00:00:00').toLocaleDateString('pt-BR')}
                        </p>
                      )}
                      {podeRegular && !encerrado && (
                        <div className="flex gap-2">
                          {etapa.status !== 'EM_ANDAMENTO' && etapa.status !== 'REALIZADO' && (
                            <button
                              onClick={() => marcarEtapa(etapa.id, 'EM_ANDAMENTO')}
                              disabled={marcandoEtapaId !== null || outraEmAndamento || anteriorPendente}
                              title={
                                anteriorPendente
                                  ? 'Conclua primeiro a etapa anterior.'
                                  : outraEmAndamento
                                    ? 'Já existe uma etapa em andamento. Conclua-a antes de iniciar outra.'
                                    : undefined
                              }
                              className="text-xs rounded-lg border px-2.5 py-1 text-gray-600 hover:bg-gray-50 disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                              Marcar em andamento
                            </button>
                          )}
                          {etapa.status !== 'REALIZADO' && (
                            <button
                              onClick={() => marcarEtapa(etapa.id, 'REALIZADO')}
                              disabled={marcandoEtapaId !== null || anteriorPendente}
                              title={anteriorPendente ? 'Conclua primeiro a etapa anterior.' : undefined}
                              className="text-xs rounded-lg border px-2.5 py-1 text-brand-teal border-brand-teal hover:bg-teal-50 disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                              Marcar como realizado
                            </button>
                          )}
                          {etapa.status === 'REALIZADO' && (
                            <button
                              onClick={() => marcarEtapa(etapa.id, 'AGUARDANDO')}
                              disabled={marcandoEtapaId !== null || posteriorIniciada}
                              title={posteriorIniciada ? 'A etapa seguinte já foi iniciada.' : undefined}
                              className="text-xs rounded-lg border px-2.5 py-1 text-gray-500 hover:bg-gray-50 disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                              Reabrir
                            </button>
                          )}
                        </div>
                      )}
                    </li>
                  )
                })}
              </ol>
            )}
          </section>

          <section className="rounded-2xl border bg-white shadow-sm p-6">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">Histórico de status</h2>
            {protocolo.historico.length === 0 ? (
              <p className="text-sm text-gray-400">Sem alterações registradas ainda.</p>
            ) : (
              <ul className="space-y-3 text-sm">
                {[...protocolo.historico].reverse().map((h, idx) => (
                  <li key={idx} className="border-b last:border-0 pb-3 last:pb-0">
                    <p className="text-gray-900">
                      {h.statusAnterior ? `${h.statusAnterior} → ${h.statusNovo}` : `Criado como ${h.statusNovo}`}
                    </p>
                    {h.observacao && <p className="text-gray-500 text-xs mt-0.5">{h.observacao}</p>}
                    <p className="text-gray-400 text-xs mt-0.5">
                      {new Date(h.criadoEm).toLocaleString('pt-BR')}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="rounded-2xl border bg-white shadow-sm p-6">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">Histórico de prioridade</h2>
            {protocolo.historicoPrioridade.length === 0 ? (
              <p className="text-sm text-gray-400">Sem reclassificações registradas ainda.</p>
            ) : (
              <ul className="space-y-3 text-sm">
                {[...protocolo.historicoPrioridade].reverse().map((h, idx) => (
                  <li key={idx} className="border-b last:border-0 pb-3 last:pb-0">
                    <p className="text-gray-900">
                      {h.prioridadeAnterior} → {h.prioridadeNova}
                    </p>
                    <p className="text-gray-500 text-xs mt-0.5">Motivo: {h.motivo}</p>
                    <p className="text-gray-400 text-xs mt-0.5">
                      {new Date(h.criadoEm).toLocaleString('pt-BR')}
                      {h.usuarioNome ? ` — por ${h.usuarioNome}` : ''}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>

        <div className="space-y-6">
          {podeRegular && (
            <>
              {(protocolo.status === 'AGUARDANDO' || protocolo.status === 'AGENDADO') && (
                <section className="rounded-2xl border bg-white shadow-sm p-6">
                  <div className="flex items-center justify-between mb-1">
                    <h2 className="text-sm font-semibold text-gray-700">
                      {protocolo.status === 'AGENDADO' ? 'Remarcar horário' : 'Agendar horário'}
                    </h2>
                    <button
                      onClick={() => setMostrarAgendamento((v) => !v)}
                      className="text-xs text-brand-navy font-semibold hover:underline"
                    >
                      {mostrarAgendamento ? 'Cancelar' : protocolo.status === 'AGENDADO' ? 'Remarcar' : 'Agendar'}
                    </button>
                  </div>
                  <p className="text-xs text-gray-400 mb-3">
                    Escolha um horário real (com vaga) para {protocolo.nomeProcedimento}
                    {protocolo.especialidadeProcedimento ? ` (${protocolo.especialidadeProcedimento})` : ''}.
                  </p>

                  {mostrarAgendamento && (
                    <div className="space-y-3">
                      <select
                        value={unidadeAgendamento}
                        onChange={(e) => setUnidadeAgendamento(e.target.value ? Number(e.target.value) : '')}
                        disabled={protocolo.unidadeSaudeId != null}
                        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm disabled:bg-gray-50 disabled:text-gray-500"
                      >
                        <option value="">Unidade de saúde</option>
                        {unidades.map((u) => (
                          <option key={u.id} value={u.id}>
                            {u.nome}
                          </option>
                        ))}
                      </select>
                      <input
                        type="date"
                        value={dataAgenda}
                        min={hojeISO()}
                        onChange={(e) => setDataAgenda(e.target.value)}
                        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
                      />

                      {!protocolo.especialidadeProcedimento && (
                        <p className="text-xs text-amber-600">
                          Este procedimento não tem especialidade cadastrada, então não é possível buscar horários
                          compatíveis automaticamente.
                        </p>
                      )}

                      {carregandoHorarios && <p className="text-xs text-gray-400">Buscando horários…</p>}
                      {!carregandoHorarios && unidadeAgendamento && protocolo.especialidadeProcedimento && horariosDisponiveis.length === 0 && (
                        <p className="text-xs text-gray-400">
                          Nenhum horário com vaga encontrado a partir dessa data.{' '}
                          <Link to="/admin/agenda" className="text-brand-navy font-semibold hover:underline">
                            Cadastrar horários na Agenda
                          </Link>
                        </p>
                      )}

                      {!carregandoHorarios && horariosDisponiveis.length > 0 && (
                        <div className="max-h-48 overflow-y-auto space-y-1.5 border rounded-lg p-2">
                          {horariosDisponiveis.map((h) => (
                            <label
                              key={h.id}
                              className={`flex items-center justify-between gap-2 rounded-lg px-2.5 py-1.5 text-xs cursor-pointer ${
                                horarioSelecionado === h.id ? 'bg-teal-50 border border-brand-teal' : 'hover:bg-gray-50'
                              } ${h.vagasDisponiveis === 0 ? 'opacity-40 cursor-not-allowed' : ''}`}
                            >
                              <span className="flex items-center gap-2">
                                <input
                                  type="radio"
                                  name="horarioSelecionado"
                                  disabled={h.vagasDisponiveis === 0}
                                  checked={horarioSelecionado === h.id}
                                  onChange={() => setHorarioSelecionado(h.id)}
                                />
                                {new Date(h.data + 'T00:00:00').toLocaleDateString('pt-BR')} às {h.horaInicio.slice(0, 5)}
                              </span>
                              <span className="font-semibold text-gray-500">
                                {h.vagasDisponiveis > 0 ? `${h.vagasDisponiveis} vaga(s)` : 'lotado'}
                              </span>
                            </label>
                          ))}
                        </div>
                      )}

                      {erroAgendamento && <p className="text-xs text-red-600">{erroAgendamento}</p>}

                      <button
                        onClick={confirmarAgendamento}
                        disabled={agendando || !horarioSelecionado}
                        className="w-full btn-teal rounded-lg text-white py-2 text-sm font-semibold disabled:opacity-40"
                      >
                        {agendando ? 'Confirmando…' : 'Confirmar agendamento'}
                      </button>
                    </div>
                  )}
                </section>
              )}

              {!encerrado && (
              <section className="rounded-2xl border bg-white shadow-sm p-6">
                <h2 className="text-sm font-semibold text-gray-700 mb-4">Alterar prioridade</h2>
                <select
                  value={novaCategoria}
                  onChange={(e) => setNovaCategoria(e.target.value as CategoriaPrioridade)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm mb-3"
                >
                  {CATEGORIAS_PRIORIDADE.map((c) => (
                    <option key={c.valor} value={c.valor}>
                      {c.rotulo}
                    </option>
                  ))}
                </select>
                {novaCategoria === 'JUDICIAL' && (
                  <input
                    type="text"
                    placeholder="Nº do processo judicial"
                    value={processoJudicial}
                    onChange={(e) => setProcessoJudicial(e.target.value)}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm mb-3"
                  />
                )}
                <textarea
                  placeholder="Motivo da reclassificação (obrigatório)"
                  value={motivoPrioridade}
                  onChange={(e) => setMotivoPrioridade(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm mb-3"
                  rows={3}
                />
                <button
                  onClick={alterarPrioridade}
                  disabled={salvandoPrioridade || novaCategoria === protocolo.categoriaPrioridade}
                  className="w-full btn-primary rounded-lg text-white py-2 text-sm font-semibold disabled:opacity-40"
                >
                  {salvandoPrioridade ? 'Salvando…' : 'Salvar nova prioridade'}
                </button>
                <p className="text-xs text-gray-400 mt-2">
                  A reclassificação fica registrada com data, usuário e motivo para auditoria.
                </p>
              </section>
              )}

              <section className="rounded-2xl border bg-white shadow-sm p-6">
                <h2 className="text-sm font-semibold text-gray-700 mb-4">Alterar status</h2>
                {TRANSICOES_MANUAIS[protocolo.status].length === 0 ? (
                  <p className="text-sm text-gray-500">
                    Este protocolo está {ROTULO_STATUS[protocolo.status].toLowerCase()} e não aceita novas mudanças de status.
                  </p>
                ) : (
                  <>
                    <select
                      value={novoStatus}
                      onChange={(e) => setNovoStatus(e.target.value as StatusProtocolo)}
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm mb-3"
                    >
                      <option value={protocolo.status}>{ROTULO_STATUS[protocolo.status]} (atual)</option>
                      {TRANSICOES_MANUAIS[protocolo.status].map((valor) => (
                        <option key={valor} value={valor}>
                          {ROTULO_STATUS[valor]}
                        </option>
                      ))}
                    </select>
                    <textarea
                      placeholder={
                        novoStatus === 'CANCELADO' ? 'Motivo do cancelamento (obrigatório)' : 'Observação (opcional)'
                      }
                      value={observacao}
                      onChange={(e) => setObservacao(e.target.value)}
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm mb-3"
                      rows={3}
                    />
                    <button
                      onClick={alterarStatus}
                      disabled={salvandoStatus || novoStatus === protocolo.status}
                      className="w-full btn-primary rounded-lg text-white py-2 text-sm font-semibold disabled:opacity-40"
                    >
                      {salvandoStatus ? 'Salvando…' : 'Salvar novo status'}
                    </button>
                    <p className="text-xs text-gray-400 mt-2">
                      O cidadão é notificado por e-mail automaticamente quando o status muda.
                      {protocolo.status === 'AGUARDANDO' && ' Para agendar, use "Agendar horário" ou "Distribuir vagas" na Fila.'}
                      {protocolo.status === 'AGENDADO' && ' Para trocar o dia ou a hora, use "Remarcar horário".'}
                    </p>
                  </>
                )}
              </section>
            </>
          )}
        </div>
      </div>
    </AdminLayout>
  )
}
