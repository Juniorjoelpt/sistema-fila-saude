import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import type { LogAuditoria, PageResponse } from '../api/types'

const ACOES: { valor: string; rotulo: string }[] = [
  { valor: '', rotulo: 'Todas as ações' },
  { valor: 'LOGIN', rotulo: 'Login' },
  { valor: 'LOGIN_FALHA', rotulo: 'Falha de login' },
  { valor: 'CRIAR_USUARIO', rotulo: 'Usuário criado' },
  { valor: 'EDITAR_USUARIO', rotulo: 'Usuário editado' },
  { valor: 'CRIAR_PACIENTE', rotulo: 'Paciente cadastrado' },
  { valor: 'CRIAR_PROTOCOLO', rotulo: 'Protocolo criado' },
  { valor: 'CRIAR_PROCEDIMENTO', rotulo: 'Procedimento cadastrado' },
  { valor: 'CRIAR_UNIDADE', rotulo: 'Unidade cadastrada' },
  { valor: 'IMPORTACAO_LOTE', rotulo: 'Importação em lote' },
]

const ROTULO_ACAO: Record<string, string> = Object.fromEntries(
  ACOES.filter((a) => a.valor).map((a) => [a.valor, a.rotulo]),
)

/**
 * Consulta ao log de auditoria geral do sistema (item 3.5 do levantamento
 * de requisitos): registro amplo de ações administrativas, restrito a
 * Admin. Complementa os históricos específicos já existentes na tela de
 * detalhe do protocolo (status e prioridade) e de cotas.
 */
export function Auditoria() {
  const [pagina, setPagina] = useState<PageResponse<LogAuditoria> | null>(null)
  const [acao, setAcao] = useState('')
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [pageIndex, setPageIndex] = useState(0)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    api
      .get<PageResponse<LogAuditoria>>('/api/admin/auditoria', {
        params: {
          acao: acao || undefined,
          dataInicio: dataInicio || undefined,
          dataFim: dataFim || undefined,
          page: pageIndex,
          size: 20,
        },
      })
      .then((res) => setPagina(res.data))
      .catch(() => setErro('Não foi possível carregar o log de auditoria.'))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [acao, dataInicio, dataFim, pageIndex])

  return (
    <AdminLayout>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Log de Auditoria</h1>
      <p className="text-sm text-gray-500 mb-6">
        Registro amplo de ações do sistema — login, cadastros, protocolos e usuários — para auditoria e
        transparência da gestão.
      </p>

      {erro && <p className="text-sm text-red-600 mb-4">{erro}</p>}

      <div className="flex flex-wrap gap-3 mb-4">
        <select
          value={acao}
          onChange={(e) => {
            setPageIndex(0)
            setAcao(e.target.value)
          }}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        >
          {ACOES.map((a) => (
            <option key={a.valor} value={a.valor}>
              {a.rotulo}
            </option>
          ))}
        </select>
        <input
          type="date"
          value={dataInicio}
          onChange={(e) => {
            setPageIndex(0)
            setDataInicio(e.target.value)
          }}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        <input
          type="date"
          value={dataFim}
          onChange={(e) => {
            setPageIndex(0)
            setDataFim(e.target.value)
          }}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
      </div>

      <div className="rounded-2xl border bg-white shadow-sm overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-xs text-gray-500 uppercase">
            <tr>
              <th className="px-4 py-3">Data/hora</th>
              <th className="px-4 py-3">Ação</th>
              <th className="px-4 py-3">Usuário</th>
              <th className="px-4 py-3">Detalhe</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {pagina?.content.length === 0 && (
              <tr>
                <td colSpan={4} className="px-4 py-6 text-center text-gray-400">
                  Nenhum registro encontrado.
                </td>
              </tr>
            )}
            {pagina?.content.map((log) => (
              <tr key={log.id}>
                <td className="px-4 py-3 text-gray-500 whitespace-nowrap">
                  {new Date(log.criadoEm).toLocaleString('pt-BR')}
                </td>
                <td className="px-4 py-3">
                  <span className="inline-flex items-center rounded-full bg-gray-100 text-gray-700 px-2.5 py-1 text-xs font-semibold">
                    {ROTULO_ACAO[log.acao] ?? log.acao}
                  </span>
                </td>
                <td className="px-4 py-3 text-gray-700">
                  {log.usuarioNome ?? log.usuarioEmail ?? '—'}
                </td>
                <td className="px-4 py-3 text-gray-500">{log.detalhe ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {pagina && pagina.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 text-sm text-gray-500">
          <span>
            Página {pagina.page + 1} de {pagina.totalPages} · {pagina.totalElements} registros
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
