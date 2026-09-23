import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import { CategoriaBadge, StatusBadge } from '../components/StatusBadge'
import type { PageResponse, Protocolo } from '../api/types'

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
export function MeusPacientes() {
  const navigate = useNavigate()
  const [pagina, setPagina] = useState<PageResponse<Protocolo> | null>(null)
  const [pageIndex, setPageIndex] = useState(0)
  const [erro, setErro] = useState<string | null>(null)
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    setCarregando(true)
    api
      .get<PageResponse<Protocolo>>('/api/fila', { params: { page: pageIndex, size: 15 } })
      .then((res) => setPagina(res.data))
      .catch(() => setErro('Não foi possível carregar seus pacientes.'))
      .finally(() => setCarregando(false))
  }, [pageIndex])

  return (
    <AdminLayout>
      <div className="flex flex-wrap items-center justify-between gap-3 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Meus Pacientes</h1>
          <p className="text-sm text-gray-500 mt-1">
            Acompanhamento dos protocolos dos pacientes da sua área.
          </p>
        </div>
        <Link
          to="/admin/novo-protocolo"
          className="rounded-lg bg-brand-navy text-white px-4 py-2 text-sm font-semibold hover:bg-brand-navy-dark"
        >
          + Cadastrar paciente / protocolo
        </Link>
      </div>

      {erro && <p className="text-sm text-red-600 mb-4">{erro}</p>}

      <div className="rounded-2xl border bg-white shadow-sm overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-xs text-gray-500 uppercase">
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
            {!carregando && pagina?.content.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-400">
                  Nenhum paciente vinculado a você ainda. Use "Cadastrar paciente / protocolo" para começar.
                </td>
              </tr>
            )}
            {pagina?.content.map((protocolo) => (
              <tr
                key={protocolo.id}
                className="cursor-pointer hover:bg-gray-50"
                onClick={() => navigate(`/admin/fila/${protocolo.id}`)}
              >
                <td className="px-4 py-3 text-gray-900 font-medium">{protocolo.nomePaciente}</td>
                <td className="px-4 py-3 text-gray-600">{protocolo.nomeProcedimento}</td>
                <td className="px-4 py-3">
                  <CategoriaBadge categoria={protocolo.categoriaPrioridade} />
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
