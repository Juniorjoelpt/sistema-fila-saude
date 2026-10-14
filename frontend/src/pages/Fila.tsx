import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Download, Plus } from 'lucide-react'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import { CategoriaBadge, PrazoBadge, StatusBadge, type NivelPrazo } from '../components/StatusBadge'
import { Spinner } from '../components/Spinner'
import type {
  CategoriaPrioridade,
  PageResponse,
  Procedimento,
  Protocolo,
  SlaConfiguracoes,
  StatusProtocolo,
  Usuario,
} from '../api/types'

const CATEGORIAS: CategoriaPrioridade[] = ['URGENCIA', 'JUDICIAL', 'ESPECIAL', 'LEGAL', 'NORMAL']
const STATUS: StatusProtocolo[] = ['AGUARDANDO', 'AGENDADO', 'EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO']

// Limiares usados até o backend carregar a configuração de SLA (item do
// roadmap "SLA configurável por especialidade/procedimento") -- mesmos
// valores padrão do backend (filasaude.sla.dias-limite-atencao/atrasado),
// só para o primeiro render não ficar sem badge.
const SLA_PADRAO: SlaConfiguracoes = { diasAtencaoPadrao: 7, diasAtrasadoPadrao: 15, configuracoes: [] }

function nivelPrazo(protocolo: Protocolo, sla: SlaConfiguracoes): NivelPrazo {
  const porProcedimento = sla.configuracoes.find((c) => c.procedimentoId === protocolo.procedimentoId)
  const porEspecialidade = sla.configuracoes.find((c) => c.especialidade === protocolo.especialidadeProcedimento)
  const config = porProcedimento ?? porEspecialidade
  const diasAtencao = config?.diasAtencao ?? sla.diasAtencaoPadrao
  const diasAtrasado = config?.diasAtrasado ?? sla.diasAtrasadoPadrao

  const dias = protocolo.diasEmEspera
  if (dias <= diasAtencao) return 'no-prazo'
  if (dias <= diasAtrasado) return 'atencao'
  return 'atrasado'
}

// Data de hoje no fuso do navegador (toISOString usaria UTC e, à noite no Brasil,
// já devolveria "amanhã").
function hojeLocal(): string {
  const d = new Date()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd}`
}

export function Fila() {
  const navigate = useNavigate()
  const [pagina, setPagina] = useState<PageResponse<Protocolo> | null>(null)
  const [categoria, setCategoria] = useState<CategoriaPrioridade | ''>('')
  const [status, setStatus] = useState<StatusProtocolo | ''>('AGUARDANDO')
  const [procedimentoId, setProcedimentoId] = useState<number | ''>('')
  const [acsResponsavelId, setAcsResponsavelId] = useState<number | ''>('')
  const [pageIndex, setPageIndex] = useState(0)
  const [carregando, setCarregando] = useState(false)

  const [procedimentos, setProcedimentos] = useState<Procedimento[]>([])
  const [acsLista, setAcsLista] = useState<Usuario[]>([])
  const [sla, setSla] = useState<SlaConfiguracoes>(SLA_PADRAO)

  const [selecionados, setSelecionados] = useState<Set<number>>(new Set())
  const [dataPrevista, setDataPrevista] = useState('')
  const [distribuindo, setDistribuindo] = useState(false)
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [erroFila, setErroFila] = useState<string | null>(null)

  useEffect(() => {
    api.get<Procedimento[]>('/api/procedimentos').then((res) => setProcedimentos(res.data))
    api.get<Usuario[]>('/api/usuarios', { params: { papel: 'ACS' } }).then((res) => setAcsLista(res.data))
    // Limiares de prazo (badge "No prazo"/"Atenção"/"Atrasado") configuráveis
    // por especialidade/procedimento -- ver página "Configurações de SLA".
    // Falha aqui não impede ver a fila, só mantém o padrão (SLA_PADRAO).
    api.get<SlaConfiguracoes>('/api/sla-configuracoes').then((res) => setSla(res.data)).catch(() => {})
  }, [])

  // Bug alto de revisão corrigido: requisições concorrentes (trocar de filtro
  // rapidamente) não eram canceladas -- uma resposta antiga podia chegar
  // depois da mais recente e "voltar no tempo" a tela. Cancela a anterior
  // sempre que uma nova busca começa.
  const controllerRef = useRef<AbortController | null>(null)

  function carregarFila() {
    controllerRef.current?.abort()
    const controller = new AbortController()
    controllerRef.current = controller

    setCarregando(true)
    setErroFila(null)
    api
      .get<PageResponse<Protocolo>>('/api/fila', {
        params: {
          categoria: categoria || undefined,
          status: status || undefined,
          procedimentoId: procedimentoId || undefined,
          acsResponsavelId: acsResponsavelId || undefined,
          page: pageIndex,
          size: 12,
        },
        signal: controller.signal,
      })
      .then((res) => setPagina(res.data))
      .catch((err) => {
        // Bug alto de revisão corrigido: erro de rede/500 deixava a tabela
        // em branco sem nenhuma mensagem -- parecia "fila vazia" para quem
        // estava operando, quando na verdade era uma falha de carregamento.
        if (err?.code === 'ERR_CANCELED') return
        setErroFila('Não foi possível carregar a fila. Tente novamente em instantes.')
      })
      .finally(() => {
        if (controllerRef.current === controller) {
          setCarregando(false)
        }
      })
  }

  useEffect(() => {
    carregarFila()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [categoria, status, procedimentoId, acsResponsavelId, pageIndex])

  useEffect(() => () => controllerRef.current?.abort(), [])

  function alternarSelecao(id: number) {
    setSelecionados((atual) => {
      const novo = new Set(atual)
      if (novo.has(id)) {
        novo.delete(id)
      } else {
        novo.add(id)
      }
      return novo
    })
  }

  const [exportando, setExportando] = useState(false)

  async function exportarPlanilha() {
    setExportando(true)
    try {
      const res = await api.get('/api/fila/export', {
        params: {
          categoria: categoria || undefined,
          status: status || undefined,
          procedimentoId: procedimentoId || undefined,
          acsResponsavelId: acsResponsavelId || undefined,
        },
        responseType: 'blob',
      })
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `agenda-saude-${hojeLocal()}.csv`)
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch {
      setMensagem('Não foi possível exportar a planilha.')
    } finally {
      setExportando(false)
    }
  }

  async function distribuirVagas() {
    if (selecionados.size === 0) return
    if (!dataPrevista) {
      setMensagem('Escolha a data prevista antes de distribuir as vagas.')
      return
    }
    if (dataPrevista < hojeLocal()) {
      setMensagem('A data prevista não pode estar no passado.')
      return
    }
    setDistribuindo(true)
    setMensagem(null)
    try {
      await api.post('/api/fila/distribuir-vagas', {
        protocoloIds: Array.from(selecionados),
        dataPrevista: dataPrevista || null,
      })
      setMensagem(`${selecionados.size} vaga(s) distribuída(s) com sucesso.`)
      setSelecionados(new Set())
      setDataPrevista('')
      carregarFila()
    } catch (err: any) {
      // Agora o backend rejeita protocolos que não estejam AGUARDANDO (ex.: já
      // CANCELADO/CONCLUIDO) -- mostrar a mensagem real diz quais remover da seleção.
      setMensagem(err?.response?.data?.mensagem ?? 'Não foi possível distribuir as vagas selecionadas.')
    } finally {
      setDistribuindo(false)
    }
  }

  return (
    <AdminLayout>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Fila de Regulação</h1>
          <p className="text-sm text-gray-400 mt-0.5">Priorização automática e agendamento dos protocolos.</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={exportarPlanilha}
            disabled={exportando}
            className="flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white text-gray-700 px-4 py-2 text-sm font-semibold disabled:opacity-40"
          >
            <Download className="h-4 w-4" strokeWidth={2.25} />
            {exportando ? 'Exportando…' : 'Exportar planilha'}
          </button>
          <button
            onClick={() => navigate('/admin/novo-protocolo')}
            className="flex items-center gap-1.5 btn-primary rounded-lg text-white px-4 py-2 text-sm font-semibold"
          >
            <Plus className="h-4 w-4" strokeWidth={2.5} />
            Novo Protocolo
          </button>
        </div>
      </div>

      <div className="flex flex-wrap gap-3 mb-5">
        <select
          value={categoria}
          onChange={(e) => {
            setCategoria(e.target.value as CategoriaPrioridade | '')
            setPageIndex(0)
          }}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        >
          <option value="">Todas as categorias</option>
          {CATEGORIAS.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>

        <select
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as StatusProtocolo | '')
            setPageIndex(0)
          }}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        >
          <option value="">Todos os status</option>
          {STATUS.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>

        <select
          value={procedimentoId}
          onChange={(e) => {
            setProcedimentoId(e.target.value ? Number(e.target.value) : '')
            setPageIndex(0)
          }}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        >
          <option value="">Todos os procedimentos</option>
          {procedimentos.map((p) => (
            <option key={p.id} value={p.id}>
              {p.nome}
            </option>
          ))}
        </select>

        <select
          value={acsResponsavelId}
          onChange={(e) => {
            setAcsResponsavelId(e.target.value ? Number(e.target.value) : '')
            setPageIndex(0)
          }}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        >
          <option value="">Todos os ACS</option>
          {acsLista.map((a) => (
            <option key={a.id} value={a.id}>
              {a.nome}
            </option>
          ))}
        </select>
      </div>

      {selecionados.size > 0 && (
        <div className="flex flex-wrap items-center gap-3 mb-4 rounded-xl border border-brand-teal bg-teal-50 px-4 py-3">
          <span className="text-sm font-semibold text-brand-navy">
            {selecionados.size} protocolo(s) selecionado(s)
          </span>
          <input
            type="date"
            value={dataPrevista}
            min={hojeLocal()}
            aria-label="Data prevista"
            onChange={(e) => setDataPrevista(e.target.value)}
            className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm"
          />
          <button
            onClick={distribuirVagas}
            disabled={distribuindo}
            className="btn-teal rounded-lg text-white px-4 py-1.5 text-sm font-semibold disabled:opacity-40"
          >
            {distribuindo ? 'Distribuindo…' : 'Distribuir Vagas'}
          </button>
          <button
            onClick={() => setSelecionados(new Set())}
            className="text-sm text-gray-500 hover:text-gray-700"
          >
            Limpar seleção
          </button>
        </div>
      )}

      {mensagem && <p className="text-sm text-gray-600 mb-4">{mensagem}</p>}

      {erroFila && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center justify-between gap-3">
          <span>{erroFila}</span>
          <button onClick={carregarFila} className="font-semibold underline shrink-0">
            Tentar novamente
          </button>
        </div>
      )}

      <div className="rounded-2xl border bg-white shadow-sm overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
            <tr>
              <th className="px-4 py-3 w-10"></th>
              <th className="text-left px-4 py-3">Paciente</th>
              <th className="text-left px-4 py-3">Procedimento</th>
              <th className="text-left px-4 py-3">Prioridade</th>
              <th className="text-left px-4 py-3">Status / Dias</th>
              <th className="text-left px-4 py-3">Prazo</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {carregando && (
              <tr>
                <td colSpan={6} className="px-4 py-10 text-center text-gray-400">
                  <div className="flex items-center justify-center gap-2">
                    <Spinner />
                    Carregando…
                  </div>
                </td>
              </tr>
            )}
            {!carregando && !erroFila && pagina?.content.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-400">
                  Nenhum protocolo encontrado para os filtros selecionados.
                </td>
              </tr>
            )}
            {!carregando &&
              pagina?.content.map((protocolo) => {
                const prazo = nivelPrazo(protocolo, sla)
                return (
                  <tr key={protocolo.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        checked={selecionados.has(protocolo.id)}
                        onChange={() => alternarSelecao(protocolo.id)}
                        className="h-4 w-4"
                      />
                    </td>
                    <td className="px-4 py-3 cursor-pointer" onClick={() => navigate(`/admin/fila/${protocolo.id}`)}>
                      <p className="font-semibold text-gray-900">{protocolo.nomePaciente}</p>
                      <p className="text-xs text-gray-400 font-mono">{protocolo.numeroProtocolo}</p>
                    </td>
                    <td className="px-4 py-3 text-gray-600 cursor-pointer" onClick={() => navigate(`/admin/fila/${protocolo.id}`)}>
                      {protocolo.nomeProcedimento}
                    </td>
                    <td className="px-4 py-3 cursor-pointer" onClick={() => navigate(`/admin/fila/${protocolo.id}`)}>
                      <CategoriaBadge categoria={protocolo.categoriaPrioridade} />
                    </td>
                    <td className="px-4 py-3 cursor-pointer" onClick={() => navigate(`/admin/fila/${protocolo.id}`)}>
                      <StatusBadge status={protocolo.status} />
                      <p className="text-xs text-gray-400 mt-1">{protocolo.diasEmEspera} dias</p>
                    </td>
                    <td
                      className="px-4 py-3 cursor-pointer"
                      onClick={() => navigate(`/admin/fila/${protocolo.id}`)}
                    >
                      <PrazoBadge nivel={prazo} />
                    </td>
                  </tr>
                )
              })}
          </tbody>
        </table>
      </div>

      {pagina && pagina.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 text-sm text-gray-500">
          <span>
            Página {pagina.page + 1} de {pagina.totalPages} · {pagina.totalElements} protocolos
          </span>
          <div className="flex gap-2">
            <button
              disabled={pageIndex === 0}
              onClick={() => setPageIndex((p) => p - 1)}
              className="rounded-lg border px-3 py-1.5 disabled:opacity-40"
            >
              Anterior
            </button>
            <button
              disabled={pageIndex + 1 >= pagina.totalPages}
              onClick={() => setPageIndex((p) => p + 1)}
              className="rounded-lg border px-3 py-1.5 disabled:opacity-40"
            >
              Próxima
            </button>
          </div>
        </div>
      )}
    </AdminLayout>
  )
}
