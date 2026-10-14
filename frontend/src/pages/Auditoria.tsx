import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { baixarExportacao, type ResultadoExportacao } from '../api/exportacao'
import { AdminLayout } from '../components/AdminLayout'
import type { LogAuditoria, PageResponse } from '../api/types'

const ACOES: { valor: string; rotulo: string }[] = [
  { valor: '', rotulo: 'Todas as ações' },
  // Acesso
  { valor: 'LOGIN', rotulo: 'Login' },
  { valor: 'LOGIN_FALHA', rotulo: 'Falha de login' },
  { valor: 'LOGIN_2FA_PENDENTE', rotulo: 'Login aguardando 2º fator' },
  { valor: 'LOGIN_2FA_FALHA', rotulo: 'Falha no 2º fator' },
  { valor: 'ALTERAR_SENHA', rotulo: 'Senha alterada pelo usuário' },
  { valor: '2FA_ATIVADO', rotulo: '2FA ativado' },
  { valor: '2FA_DESATIVADO', rotulo: '2FA desativado' },
  // Protocolos e fila
  { valor: 'CRIAR_PROTOCOLO', rotulo: 'Protocolo criado' },
  { valor: 'MUDAR_STATUS_PROTOCOLO', rotulo: 'Status do protocolo alterado' },
  { valor: 'ALTERAR_PRIORIDADE_PROTOCOLO', rotulo: 'Prioridade reclassificada' },
  { valor: 'MARCAR_ETAPA_PROTOCOLO', rotulo: 'Etapa do protocolo alterada' },
  { valor: 'AGENDAR_HORARIO_PROTOCOLO', rotulo: 'Horário agendado' },
  { valor: 'DISTRIBUIR_VAGAS', rotulo: 'Vagas distribuídas em lote' },
  { valor: 'CONFIRMAR_PRESENCA', rotulo: 'Presença confirmada' },
  { valor: 'CANCELAR_PRESENCA', rotulo: 'Presença cancelada' },
  // Pacientes
  { valor: 'CRIAR_PACIENTE', rotulo: 'Paciente cadastrado' },
  { valor: 'ATUALIZAR_PACIENTE', rotulo: 'Paciente atualizado' },
  { valor: 'EXCLUIR_PACIENTE', rotulo: 'Paciente excluído' },
  { valor: 'REATRIBUIR_ACS_PACIENTE', rotulo: 'ACS do paciente reatribuído' },
  { valor: 'IMPORTACAO_LOTE', rotulo: 'Importação em lote' },
  // Cadastros e configurações
  { valor: 'CRIAR_USUARIO', rotulo: 'Usuário criado' },
  { valor: 'EDITAR_USUARIO', rotulo: 'Usuário editado' },
  { valor: 'CRIAR_PROCEDIMENTO', rotulo: 'Procedimento cadastrado' },
  { valor: 'ATUALIZAR_PROCEDIMENTO', rotulo: 'Procedimento atualizado' },
  { valor: 'DESATIVAR_PROCEDIMENTO', rotulo: 'Procedimento desativado' },
  { valor: 'CRIAR_UNIDADE', rotulo: 'Unidade cadastrada' },
  { valor: 'ATUALIZAR_UNIDADE', rotulo: 'Unidade atualizada' },
  { valor: 'DESATIVAR_UNIDADE', rotulo: 'Unidade desativada' },
  { valor: 'CRIAR_ESPECIALIDADE', rotulo: 'Especialidade cadastrada' },
  { valor: 'ATUALIZAR_ESPECIALIDADE', rotulo: 'Especialidade atualizada' },
  { valor: 'DESATIVAR_ESPECIALIDADE', rotulo: 'Especialidade desativada' },
  { valor: 'AJUSTAR_COTA', rotulo: 'Cota ajustada' },
  { valor: 'CRIAR_HORARIO_AGENDA', rotulo: 'Horário de agenda criado' },
  { valor: 'GERAR_LOTE_HORARIOS_AGENDA', rotulo: 'Lote de horários gerado' },
  { valor: 'REMOVER_HORARIO_AGENDA', rotulo: 'Horário de agenda removido' },
  { valor: 'CRIAR_SLA_CONFIGURACAO', rotulo: 'Regra de SLA criada' },
  { valor: 'ATUALIZAR_SLA_CONFIGURACAO', rotulo: 'Regra de SLA atualizada' },
  { valor: 'EXCLUIR_SLA_CONFIGURACAO', rotulo: 'Regra de SLA excluída' },
  { valor: 'ATUALIZAR_INTEGRACAO', rotulo: 'Integração atualizada' },
  { valor: 'EXPORTAR_AUDITORIA', rotulo: 'Trilha de auditoria exportada' },
  { valor: 'EXPORTAR_DOSSIE_PROTOCOLO', rotulo: 'Dossiê de protocolo exportado' },
  { valor: 'PUBLICAR_TRANSPARENCIA', rotulo: 'Transparência publicada' },
  { valor: 'DESPUBLICAR_TRANSPARENCIA', rotulo: 'Transparência despublicada' },
  { valor: 'ATUALIZAR_TRANSPARENCIA', rotulo: 'Transparência atualizada' },
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
  const [exportando, setExportando] = useState<'pdf' | 'csv' | 'json' | null>(null)
  const [exportado, setExportado] = useState<(ResultadoExportacao & { formato: string }) | null>(null)

  async function exportar(formato: 'pdf' | 'csv' | 'json') {
    setExportando(formato)
    setErro(null)
    setExportado(null)
    try {
      const r = await baixarExportacao(
        '/api/admin/auditoria/export',
        { formato, acao: acao || undefined, dataInicio: dataInicio || undefined, dataFim: dataFim || undefined },
        `trilha-auditoria.${formato}`,
        'Não foi possível exportar a trilha de auditoria.',
      )
      setExportado({ ...r, formato })
    } catch (e: any) {
      setErro(e.message)
    } finally {
      setExportando(null)
    }
  }

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

      <div className="rounded-2xl border bg-white shadow-sm p-4 mb-4">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-sm font-semibold text-gray-700 mr-2">Exportar para TCE/MP</span>
          {(['pdf', 'csv', 'json'] as const).map((f) => (
            <button
              key={f}
              onClick={() => exportar(f)}
              disabled={exportando !== null}
              className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-50"
            >
              {exportando === f ? 'Gerando…' : f === 'pdf' ? 'Relatório PDF' : f === 'csv' ? 'Planilha CSV' : 'Dados JSON'}
            </button>
          ))}
        </div>
        <p className="text-xs text-gray-500 mt-2">
          Usa os filtros acima (ação e período), em ordem cronológica. Informe o período para um relatório mais objetivo.
          Cada exportação fica registrada nesta trilha, junto com o SHA-256 do arquivo.
        </p>
        {exportado && (
          <div className="mt-3 rounded-lg bg-emerald-50 border border-emerald-200 px-3 py-2 text-xs text-emerald-900">
            <p className="font-semibold">
              {exportado.nomeArquivo} gerado{exportado.total != null ? ` com ${exportado.total} registro(s)` : ''}.
            </p>
            {exportado.sha256 && (
              <p className="mt-1 break-all">
                SHA-256: <span className="font-mono">{exportado.sha256}</span> — gravado na trilha (ação “Trilha de auditoria
                exportada”). Quem receber o arquivo pode recalcular o hash e conferir.
              </p>
            )}
          </div>
        )}
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
