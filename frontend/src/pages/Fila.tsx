import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import { CategoriaBadge, StatusBadge } from '../components/StatusBadge'
import type { CategoriaPrioridade, PageResponse, Protocolo, StatusProtocolo } from '../api/types'

const CATEGORIAS: CategoriaPrioridade[] = ['URGENCIA', 'JUDICIAL', 'ESPECIAL', 'LEGAL', 'NORMAL']
const STATUS: StatusProtocolo[] = ['AGUARDANDO', 'AGENDADO', 'EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO']

function prazoLabel(dias: number): { texto: string; cor: string } {
  if (dias <= 7) return { texto: 'No Prazo', cor: 'text-green-600' }
  if (dias <= 15) return { texto: 'Atenção', cor: 'text-amber-600' }
  return { texto: 'Atrasado', cor: 'text-red-600' }
}

export function Fila() {
  const navigate = useNavigate()
  const [pagina, setPagina] = useState<PageResponse<Protocolo> | null>(null)
  const [categoria, setCategoria] = useState<CategoriaPrioridade | ''>('')
  const [status, setStatus] = useState<StatusProtocolo | ''>('AGUARDANDO')
  const [pageIndex, setPageIndex] = useState(0)
  const [carregando, setCarregando] = useState(false)

  useEffect(() => {
    setCarregando(true)
    api
      .get<PageResponse<Protocolo>>('/api/fila', {
        params: {
          categoria: categoria || undefined,
          status: status || undefined,
          page: pageIndex,
          size: 12,
        },
      })
      .then((res) => setPagina(res.data))
      .finally(() => setCarregando(false))
  }, [categoria, status, pageIndex])

  return (
    <AdminLayout>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Fila de Regulação</h1>
        <button
          onClick={() => navigate('/admin/novo-protocolo')}
          className="rounded-lg bg-brand-navy text-white px-4 py-2 text-sm font-semibold"
        >
          + Novo Protocolo
        </button>
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
      </div>

      <div className="rounded-2xl border bg-white shadow-sm overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
            <tr>
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
                <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                  Carregando…
                </td>
              </tr>
            )}
            {!carregando && pagina?.content.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                  Nenhum protocolo encontrado para os filtros selecionados.
                </td>
              </tr>
            )}
            {!carregando &&
              pagina?.content.map((protocolo) => {
                const prazo = prazoLabel(protocolo.diasEmEspera)
                return (
                  <tr
                    key={protocolo.id}
                    onClick={() => navigate(`/admin/fila/${protocolo.id}`)}
                    className="hover:bg-gray-50 cursor-pointer"
                  >
                    <td className="px-4 py-3">
                      <p className="font-semibold text-gray-900">{protocolo.nomePaciente}</p>
                      <p className="text-xs text-gray-400 font-mono">{protocolo.numeroProtocolo}</p>
                    </td>
                    <td className="px-4 py-3 text-gray-600">{protocolo.nomeProcedimento}</td>
                    <td className="px-4 py-3">
                      <CategoriaBadge categoria={protocolo.categoriaPrioridade} />
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={protocolo.status} />
                      <p className="text-xs text-gray-400 mt-1">{protocolo.diasEmEspera} dias</p>
                    </td>
                    <td className={`px-4 py-3 text-xs font-semibold ${prazo.cor}`}>{prazo.texto}</td>
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
