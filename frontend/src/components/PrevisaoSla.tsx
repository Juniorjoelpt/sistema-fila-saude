import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { ROTULOS_CATEGORIA } from './StatusBadge'
import type { PrevisaoSlaResponse } from '../api/types'

const TENDENCIA: Record<string, { rotulo: string; classe: string }> = {
  CRESCENDO: { rotulo: 'Fila crescendo', classe: 'text-red-600' },
  ESTAVEL: { rotulo: 'Estável', classe: 'text-gray-500' },
  DIMINUINDO: { rotulo: 'Diminuindo', classe: 'text-emerald-600' },
  SEM_DADOS: { rotulo: 'Sem dados', classe: 'text-gray-400' },
}

/**
 * Previsão de SLA (roadmap #5): além de quem já está atrasado, mostra quem vai
 * estourar o prazo se o ritmo de atendimento dos últimos dias se mantiver.
 * Carrega por conta própria: se falhar, o restante do painel segue normal.
 */
export function PrevisaoSla() {
  const [dados, setDados] = useState<PrevisaoSlaResponse | null>(null)
  const [erro, setErro] = useState(false)

  useEffect(() => {
    api
      .get<PrevisaoSlaResponse>('/api/admin/dashboard/previsao-sla')
      .then((res) => setDados(res.data))
      .catch(() => setErro(true))
  }, [])

  if (erro) {
    return (
      <div className="rounded-2xl border bg-white shadow-sm p-6 mb-8">
        <p className="text-sm text-red-600">Não foi possível carregar a previsão de SLA.</p>
      </div>
    )
  }
  if (!dados) return null

  const { resumo } = dados

  return (
    <div className="rounded-2xl border bg-white shadow-sm p-6 mb-8">
      <div className="flex flex-wrap items-center justify-between gap-2 mb-1">
        <h2 className="text-sm font-semibold text-gray-700">Previsão de SLA</h2>
        <span className="text-xs text-gray-400">Ritmo dos últimos {dados.janelaDias} dias</span>
      </div>
      <p className="text-xs text-gray-400 mb-4">
        Estima quem vai passar do limite de SLA se a fila continuar andando no ritmo recente: a posição do
        protocolo dividida pela média diária de atendimentos do procedimento.
      </p>

      {resumo.totalAguardando === 0 ? (
        <p className="text-sm text-gray-400">Ninguém aguardando no momento.</p>
      ) : (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-5">
            <div>
              <p className="text-xs text-gray-400 uppercase tracking-wide">Já atrasados</p>
              <p className={`text-2xl font-extrabold ${resumo.jaAtrasados > 0 ? 'text-red-600' : 'text-brand-navy'}`}>
                {resumo.jaAtrasados}
              </p>
            </div>
            <div>
              <p className="text-xs text-gray-400 uppercase tracking-wide">Vão atrasar</p>
              <p className={`text-2xl font-extrabold ${resumo.vaoAtrasar > 0 ? 'text-amber-600' : 'text-brand-navy'}`}>
                {resumo.vaoAtrasar}
              </p>
            </div>
            <div>
              <p className="text-xs text-gray-400 uppercase tracking-wide">Previsto no prazo</p>
              <p className="text-2xl font-extrabold text-brand-navy">
                {resumo.percentualPrevistoNoPrazo != null ? `${resumo.percentualPrevistoNoPrazo}%` : '—'}
              </p>
            </div>
            <div>
              <p className="text-xs text-gray-400 uppercase tracking-wide">Sem histórico</p>
              <p className="text-2xl font-extrabold text-gray-500">{resumo.semHistorico}</p>
              <p className="text-[11px] text-gray-400">nenhum atendimento na janela: sem como prever</p>
            </div>
          </div>

          <div className="overflow-x-auto mb-5">
            <table className="w-full text-sm">
              <thead className="text-gray-500 text-xs uppercase">
                <tr>
                  <th className="text-left py-2 pr-3">Procedimento</th>
                  <th className="text-right py-2 px-3">Aguardando</th>
                  <th className="text-right py-2 px-3">Atendimentos/semana</th>
                  <th className="text-right py-2 px-3">Esvazia em</th>
                  <th className="text-right py-2 px-3">Atrasados</th>
                  <th className="text-right py-2 px-3">Vão atrasar</th>
                  <th className="text-left py-2 pl-3">Tendência</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {dados.porProcedimento.map((l) => {
                  const t = TENDENCIA[l.tendencia] ?? TENDENCIA.SEM_DADOS
                  return (
                    <tr key={l.procedimentoId}>
                      <td className="py-2 pr-3 text-gray-900">
                        {l.procedimento}
                        {l.especialidade && <span className="text-gray-400"> · {l.especialidade}</span>}
                      </td>
                      <td className="py-2 px-3 text-right">{l.aguardando}</td>
                      <td className="py-2 px-3 text-right">{l.saidasNaJanela > 0 ? l.vazaoSemanal : '—'}</td>
                      <td className="py-2 px-3 text-right">
                        {l.diasParaEsvaziarFila != null ? `${l.diasParaEsvaziarFila} dias` : '—'}
                      </td>
                      <td className={`py-2 px-3 text-right ${l.jaAtrasados > 0 ? 'text-red-600 font-semibold' : ''}`}>
                        {l.jaAtrasados}
                      </td>
                      <td className={`py-2 px-3 text-right ${l.vaoAtrasar > 0 ? 'text-amber-600 font-semibold' : ''}`}>
                        {l.vaoAtrasar}
                      </td>
                      <td className={`py-2 pl-3 text-xs ${t.classe}`}>
                        {t.rotulo}
                        <span className="text-gray-400"> ({l.entradasNaJanela} entraram / {l.saidasNaJanela} saíram)</span>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {dados.protocolosEmRisco.length > 0 && (
            <div>
              <h3 className="text-xs font-semibold text-gray-600 uppercase tracking-wide mb-2">
                Próximos a estourar o prazo
              </h3>
              <ul className="divide-y rounded-xl border">
                {dados.protocolosEmRisco.map((p) => (
                  <li key={p.protocoloId}>
                    <Link
                      to={`/admin/fila/${p.protocoloId}`}
                      className="flex flex-wrap items-center justify-between gap-2 px-4 py-2.5 text-sm hover:bg-gray-50"
                    >
                      <span>
                        <span className="font-medium text-gray-900">{p.nomePaciente}</span>
                        <span className="text-gray-400">
                          {' '}
                          · {p.procedimento} · {ROTULOS_CATEGORIA[p.categoriaPrioridade as keyof typeof ROTULOS_CATEGORIA] ?? p.categoriaPrioridade} · {p.posicaoFila}º na fila
                        </span>
                      </span>
                      <span className="text-xs text-amber-700 font-semibold">
                        atrasa em {p.diasAteAtraso} dia{p.diasAteAtraso === 1 ? '' : 's'} · atendimento previsto em ~
                        {p.atendimentoEstimadoEmDias} dias (limite {p.limiteDias})
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
              <p className="text-[11px] text-gray-400 mt-2">
                Para evitar o atraso: distribua vagas ou abra mais horários para estes procedimentos.
              </p>
            </div>
          )}
        </>
      )}
    </div>
  )
}
