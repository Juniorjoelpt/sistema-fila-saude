import { useEffect, useState } from 'react'
import { CalendarCheck2, Clock, Search, Siren, Users, type LucideIcon } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import { CategoriaBadge, ROTULOS, StatusBadge } from '../components/StatusBadge'
import type { CategoriaPrioridade, PageResponse, Protocolo, StatusProtocolo } from '../api/types'

const STATUS_FILTRO: StatusProtocolo[] = ['AGUARDANDO', 'AGENDADO', 'EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO']

/**
 * Área do ACS (Agente Comunitário de Saúde) -- item 2 do levantamento de
 * requisitos: "cadastra e acompanha pacientes da sua área", "vincular
 * pacientes aos protocolos, tirar dúvidas". Diferente da Fila de Regulação
 * (Regulador/Admin), aqui o ACS só acompanha o andamento dos protocolos dos
 * pacientes que ele mesmo cadastrou/atende -- sem ações de priorização,
 * mudança de status ou distribuição de vagas, que são privativas da
 * regulação.
 *
 * O filtro por ACS é aplicado automaticamente pelo backend (o ACS nunca vê
 * pacientes de outra área), então esta tela não precisa enviar nenhum
 * parâmetro de filtro -- só consome /api/fila como está.
 */
const CATEGORIAS: { valor: CategoriaPrioridade; rotulo: string }[] = [
  { valor: 'URGENCIA', rotulo: 'Urgência' },
  { valor: 'JUDICIAL', rotulo: 'Judicial' },
  { valor: 'ESPECIAL', rotulo: 'Especial' },
  { valor: 'LEGAL', rotulo: 'Legal' },
  { valor: 'NORMAL', rotulo: 'Normal' },
]

type Resumo = { total: number; aguardando: number; agendado: number; andamento: number; urgencia: number }

function CartaoResumo({
  titulo,
  valor,
  Icone,
  cor,
  ativo,
  onClick,
}: {
  titulo: string
  valor: number | null
  Icone: LucideIcon
  cor: string
  ativo: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={ativo}
      className={`text-left rounded-3xl border p-4 transition-all ${
        ativo ? 'border-brand-teal ring-2 ring-brand-teal/30 bg-white' : 'border-white/80 bg-white/70'
      }`}
      style={{ boxShadow: 'var(--shadow-sm)' }}
    >
      <span className={`flex h-9 w-9 items-center justify-center rounded-full mb-2 ${cor}`}>
        <Icone className="h-4 w-4" strokeWidth={2.25} />
      </span>
      <p className="text-[11px] text-gray-500 uppercase tracking-wide">{titulo}</p>
      <p className="text-2xl font-extrabold text-brand-navy">{valor ?? '…'}</p>
    </button>
  )
}

export function MeusPacientes() {
  const navigate = useNavigate()
  const [pagina, setPagina] = useState<PageResponse<Protocolo> | null>(null)
  const [pageIndex, setPageIndex] = useState(0)
  const [status, setStatus] = useState<StatusProtocolo | ''>('')
  const [categoria, setCategoria] = useState<CategoriaPrioridade | ''>('')
  const [buscaDigitada, setBuscaDigitada] = useState('')
  const [busca, setBusca] = useState('')
  const [resumo, setResumo] = useState<Resumo | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [carregando, setCarregando] = useState(true)

  // Debounce da busca para não consultar a API a cada tecla.
  useEffect(() => {
    const t = setTimeout(() => {
      setBusca(buscaDigitada.trim())
      setPageIndex(0)
    }, 350)
    return () => clearTimeout(t)
  }, [buscaDigitada])

  // Resumo: o backend já restringe ao ACS logado; basta ler o total de cada filtro.
  useEffect(() => {
    const total = (params: Record<string, string>) =>
      api
        .get<PageResponse<Protocolo>>('/api/fila', { params: { ...params, page: 0, size: 1 } })
        .then((r) => r.data.totalElements)
    Promise.all([
      total({}),
      total({ status: 'AGUARDANDO' }),
      total({ status: 'AGENDADO' }),
      total({ status: 'EM_ANDAMENTO' }),
      total({ categoria: 'URGENCIA' }),
    ])
      .then(([t, a, ag, an, u]) => setResumo({ total: t, aguardando: a, agendado: ag, andamento: an, urgencia: u }))
      .catch(() => setResumo(null))
  }, [])

  useEffect(() => {
    setCarregando(true)
    setErro(null)
    api
      .get<PageResponse<Protocolo>>('/api/fila', {
        params: {
          status: status || undefined,
          categoria: categoria || undefined,
          busca: busca || undefined,
          page: pageIndex,
          size: 15,
        },
      })
      .then((res) => setPagina(res.data))
      .catch(() => setErro('Não foi possível carregar seus pacientes.'))
      .finally(() => setCarregando(false))
  }, [pageIndex, status, categoria, busca])

  function filtrar(novoStatus: StatusProtocolo | '', novaCategoria: CategoriaPrioridade | '' = '') {
    setStatus(novoStatus)
    setCategoria(novaCategoria)
    setPageIndex(0)
  }

  const vazio = !carregando && pagina?.content.length === 0
  const temFiltro = status !== '' || categoria !== '' || busca !== ''

  return (
    <AdminLayout>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Meus Pacientes</h1>
        <p className="text-sm text-gray-500 mt-1">Acompanhamento dos protocolos dos pacientes da sua área.</p>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-5 gap-3 mb-6">
        <CartaoResumo titulo="Total" valor={resumo?.total ?? null} Icone={Users} cor="bg-teal-50 text-teal-600"
          ativo={!status && !categoria} onClick={() => filtrar('')} />
        <CartaoResumo titulo="Aguardando" valor={resumo?.aguardando ?? null} Icone={Clock} cor="bg-amber-50 text-amber-600"
          ativo={status === 'AGUARDANDO' && !categoria} onClick={() => filtrar('AGUARDANDO')} />
        <CartaoResumo titulo="Agendados" valor={resumo?.agendado ?? null} Icone={CalendarCheck2} cor="bg-blue-50 text-blue-600"
          ativo={status === 'AGENDADO' && !categoria} onClick={() => filtrar('AGENDADO')} />
        <CartaoResumo titulo="Em andamento" valor={resumo?.andamento ?? null} Icone={Clock} cor="bg-violet-50 text-violet-600"
          ativo={status === 'EM_ANDAMENTO' && !categoria} onClick={() => filtrar('EM_ANDAMENTO')} />
        <CartaoResumo titulo="Urgências" valor={resumo?.urgencia ?? null} Icone={Siren} cor="bg-red-50 text-red-600"
          ativo={categoria === 'URGENCIA'} onClick={() => filtrar('', 'URGENCIA')} />
      </div>

      {erro && <p className="text-sm text-red-600 mb-4">{erro}</p>}

      <div className="flex flex-wrap items-center gap-3 mb-3">
        <div className="relative flex-1 min-w-[220px]">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input
            type="search"
            value={buscaDigitada}
            onChange={(e) => setBuscaDigitada(e.target.value)}
            placeholder="Buscar por nome, CPF ou nº do protocolo"
            aria-label="Buscar paciente"
            className="w-full border pl-10 pr-3 py-2 text-sm"
          />
        </div>
        <select
          value={categoria}
          onChange={(e) => {
            setCategoria(e.target.value as CategoriaPrioridade | '')
            setPageIndex(0)
          }}
          aria-label="Filtrar por prioridade"
          className="border px-3 py-2 text-sm"
        >
          <option value="">Todas as prioridades</option>
          {CATEGORIAS.map((c) => (
            <option key={c.valor} value={c.valor}>
              {c.rotulo}
            </option>
          ))}
        </select>
      </div>

      <div className="flex flex-wrap gap-2 mb-4">
        {[{ v: '' as const, r: 'Todos' }, ...STATUS_FILTRO.map((st) => ({ v: st, r: ROTULOS[st] }))].map((c) => (
          <button
            key={c.v || 'todos'}
            type="button"
            onClick={() => {
              setStatus(c.v)
              setPageIndex(0)
            }}
            className={`rounded-full px-3.5 py-1.5 text-xs font-semibold border ${
              status === c.v ? 'bg-brand-teal text-white border-transparent shadow-md' : 'bg-white/70 text-gray-600 border-white/80'
            }`}
          >
            {c.r}
          </button>
        ))}
        {temFiltro && (
          <button
            type="button"
            onClick={() => {
              filtrar('')
              setBuscaDigitada('')
            }}
            className="rounded-full px-3.5 py-1.5 text-xs font-semibold text-gray-500 underline"
          >
            Limpar filtros
          </button>
        )}
      </div>

      {vazio && (
        <div className="rounded-2xl border bg-white shadow-sm px-4 py-10 text-center text-gray-400 text-sm">
          {temFiltro
            ? 'Nenhum paciente encontrado com esses filtros.'
            : 'Nenhum paciente vinculado a você ainda. Use "Novo Protocolo", no menu, para começar.'}
        </div>
      )}

      {/* Celular: cartões por paciente */}
      <div className="md:hidden space-y-3">
        {pagina?.content.map((p) => (
          <Link
            key={p.id}
            to={`/admin/fila/${p.id}`}
            className="block rounded-3xl border border-white/80 bg-white/75 p-4"
            style={{ boxShadow: 'var(--shadow-sm)' }}
          >
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0">
                <p className="font-semibold text-gray-900 truncate">{p.nomePaciente}</p>
                <p className="text-xs text-gray-500 truncate">{p.nomeProcedimento}</p>
              </div>
              <StatusBadge status={p.status} />
            </div>
            <div className="mt-3 flex items-center justify-between text-xs text-gray-500">
              <CategoriaBadge categoria={p.categoriaPrioridade} solicitada={p.categoriaSolicitada} />
              <span>
                {p.posicaoFila != null ? `${p.posicaoFila}º na fila · ` : ''}
                {p.diasEmEspera} dias
              </span>
            </div>
          </Link>
        ))}
      </div>

      {/* Computador: tabela */}
      <div className="hidden md:block rounded-2xl border bg-white shadow-sm overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="text-left text-xs text-gray-500 uppercase">
            <tr>
              <th className="px-4 py-3">Paciente</th>
              <th className="px-4 py-3">Procedimento</th>
              <th className="px-4 py-3">Prioridade</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Posição na fila</th>
              <th className="px-4 py-3">Dias em espera</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {pagina?.content.map((protocolo) => (
              <tr
                key={protocolo.id}
                className="cursor-pointer hover:bg-gray-50"
                onClick={() => navigate(`/admin/fila/${protocolo.id}`)}
              >
                <td className="px-4 py-3 text-gray-900 font-medium">
                  {/* Link real (além do clique na linha): navegável por teclado e leitor de tela */}
                  <Link
                    to={`/admin/fila/${protocolo.id}`}
                    onClick={(e) => e.stopPropagation()}
                    className="hover:underline focus:underline"
                  >
                    {protocolo.nomePaciente}
                  </Link>
                </td>
                <td className="px-4 py-3 text-gray-600">{protocolo.nomeProcedimento}</td>
                <td className="px-4 py-3">
                  <CategoriaBadge categoria={protocolo.categoriaPrioridade} solicitada={protocolo.categoriaSolicitada} />
                </td>
                <td className="px-4 py-3">
                  <StatusBadge status={protocolo.status} />
                </td>
                <td className="px-4 py-3 text-gray-600">
                  {protocolo.posicaoFila != null ? `${protocolo.posicaoFila}º` : '—'}
                </td>
                <td className="px-4 py-3 text-gray-600">{protocolo.diasEmEspera} dias</td>
              </tr>
            ))}
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
