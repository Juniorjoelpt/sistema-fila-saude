import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import {
  Clock,
  CalendarCheck2,
  CheckCircle2,
  Gauge,
  TrendingUp,
  type LucideIcon,
} from 'lucide-react'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import { useAuth } from '../context/AuthContext'
import type { DashboardResponse } from '../api/types'

const CORES_ICONE = [
  'bg-blue-50 text-blue-600',
  'bg-teal-50 text-teal-600',
  'bg-emerald-50 text-emerald-600',
  'bg-amber-50 text-amber-600',
  'bg-violet-50 text-violet-600',
]

function Cartao({
  titulo,
  valor,
  destaque,
  Icone,
  cor,
}: {
  titulo: string
  valor: number | string
  destaque?: string
  Icone: LucideIcon
  cor: number
}) {
  return (
    <div className="rounded-2xl border bg-white shadow-sm p-5">
      <span
        className={`flex h-9 w-9 items-center justify-center rounded-full mb-3 ${CORES_ICONE[cor % CORES_ICONE.length]}`}
      >
        <Icone className="h-4.5 w-4.5" strokeWidth={2.25} />
      </span>
      <p className="text-xs text-gray-400 uppercase tracking-wide">{titulo}</p>
      <p className="text-3xl font-extrabold text-brand-navy mt-1">{valor}</p>
      {destaque && <p className="text-xs text-brand-teal font-medium mt-1">{destaque}</p>}
    </div>
  )
}

const CORES_BARRA = ['bg-blue-500', 'bg-violet-500', 'bg-teal-500', 'bg-amber-500', 'bg-rose-500', 'bg-emerald-500']

export function Dashboard() {
  const { usuario } = useAuth()
  const [dados, setDados] = useState<DashboardResponse | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    api
      .get<DashboardResponse>('/api/admin/dashboard')
      .then((res) => setDados(res.data))
      .catch(() => setErro('Não foi possível carregar os indicadores.'))
  }, [])

  // O ACS não tem visão gerencial (item 2 do levantamento de requisitos) --
  // a área dele é "Meus Pacientes".
  if (usuario?.papel === 'ACS') {
    return <Navigate to="/admin/meus-pacientes" replace />
  }

  return (
    <AdminLayout>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Painel Administrativo</h1>
        <p className="text-sm text-gray-400 mt-0.5">Visão consolidada da operação da fila de regulação.</p>
      </div>

      {erro && <p className="text-sm text-red-600 mb-4">{erro}</p>}

      {!dados && !erro && (
        <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-5 gap-4 mb-8">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="rounded-2xl border bg-white shadow-sm p-5">
              <div className="skeleton h-9 w-9 rounded-full mb-3" />
              <div className="skeleton h-3 w-20 rounded mb-2" />
              <div className="skeleton h-7 w-14 rounded" />
            </div>
          ))}
        </div>
      )}

      {dados && (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-5 gap-4 mb-8">
            <Cartao titulo="Fila de Espera" valor={dados.filaDeEspera} destaque="Aguardando" Icone={Clock} cor={0} />
            <Cartao titulo="Agendados" valor={dados.agendadosNoMes} destaque="Neste mês" Icone={CalendarCheck2} cor={1} />
            <Cartao titulo="Realizados" valor={dados.realizadosNoAno} destaque="Total no ano" Icone={CheckCircle2} cor={2} />
            <Cartao
              titulo="Taxa de Ocupação"
              valor={dados.taxaOcupacaoGeral != null ? `${dados.taxaOcupacaoGeral}%` : '—'}
              destaque={dados.taxaOcupacaoGeral != null ? 'Cotas do mês' : 'Sem cotas cadastradas'}
              Icone={Gauge}
              cor={3}
            />
            <Cartao
              titulo="Pico Máximo"
              valor={dados.picoOcupacaoGeral != null ? `${dados.picoOcupacaoGeral}%` : '—'}
              destaque="Maior ocupação já registrada"
              Icone={TrendingUp}
              cor={4}
            />
          </div>

          {dados.ocupacaoPorEspecialidade.length > 0 && (
            <div className="rounded-2xl border bg-white shadow-sm p-6 mb-8">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-semibold text-gray-700">Ocupação de Cotas por Especialidade</h2>
                <span className="text-xs text-gray-400">Mês corrente</span>
              </div>
              <div className="space-y-3">
                {dados.ocupacaoPorEspecialidade.map((item, idx) => (
                  <div key={item.especialidade}>
                    <div className="flex justify-between text-xs mb-1">
                      <span className="font-medium text-gray-700 flex items-center gap-1.5">
                        {item.especialidade}
                        {item.gargalo && (
                          <span className="rounded-full bg-red-100 text-red-800 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide">
                            Gargalo
                          </span>
                        )}
                      </span>
                      <span className="text-gray-400">
                        {item.quantidadeUtilizada} / {item.quantidadeTotal} ({item.percentual}%)
                      </span>
                    </div>
                    <div className="h-2.5 rounded-full bg-gray-100 overflow-hidden">
                      <div
                        className={`h-full rounded-full ${item.gargalo ? 'bg-red-500' : CORES_BARRA[idx % CORES_BARRA.length]}`}
                        style={{ width: `${Math.min(100, item.percentual)}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
            <div className="rounded-2xl border bg-white shadow-sm p-6">
              <div className="flex items-center justify-between mb-1">
                <h2 className="text-sm font-semibold text-gray-700">SLA da Fila de Espera</h2>
                <span className="text-xs text-gray-400">Limite: {dados.sla.diasLimite} dias</span>
              </div>
              <p className="text-xs text-gray-400 mb-4">
                Protocolos aguardando há mais de {dados.sla.diasLimite} dias são considerados "Atrasado".
              </p>

              {dados.sla.totalAguardando === 0 ? (
                <p className="text-sm text-gray-400">Ninguém aguardando no momento.</p>
              ) : (
                <>
                  <div className="flex items-end justify-between mb-2">
                    <span className="text-3xl font-extrabold text-brand-navy">
                      {dados.sla.percentualDentroPrazo}%
                    </span>
                    <span className="text-xs text-gray-400">dentro do prazo</span>
                  </div>
                  <div className="h-2.5 rounded-full bg-gray-100 overflow-hidden mb-3">
                    <div
                      className="h-full rounded-full bg-brand-teal"
                      style={{ width: `${dados.sla.percentualDentroPrazo ?? 0}%` }}
                    />
                  </div>
                  <div className="flex justify-between text-xs">
                    <span className="text-gray-500">{dados.sla.totalDentroPrazo} dentro do prazo</span>
                    <span className={dados.sla.totalAtrasado > 0 ? 'text-red-600 font-semibold' : 'text-gray-500'}>
                      {dados.sla.totalAtrasado} atrasado{dados.sla.totalAtrasado === 1 ? '' : 's'}
                    </span>
                  </div>
                </>
              )}
            </div>

            <div className="rounded-2xl border bg-white shadow-sm p-6">
              <div className="flex items-center justify-between mb-1">
                <h2 className="text-sm font-semibold text-gray-700">Confirmação de Presença</h2>
              </div>
              <p className="text-xs text-gray-400 mb-4">
                Entre pacientes que receberam o lembrete de agendamento (e-mail/WhatsApp).
              </p>

              {dados.confirmacaoPresenca.totalLembretesEnviados === 0 ? (
                <p className="text-sm text-gray-400">Nenhum lembrete enviado ainda.</p>
              ) : (
                <>
                  <div className="flex items-end justify-between mb-3">
                    <span className="text-3xl font-extrabold text-brand-navy">
                      {dados.confirmacaoPresenca.percentualConfirmacao != null
                        ? `${dados.confirmacaoPresenca.percentualConfirmacao}%`
                        : '—'}
                    </span>
                    <span className="text-xs text-gray-400">taxa de confirmação</span>
                  </div>
                  <div className="grid grid-cols-3 gap-2 text-center">
                    <div className="rounded-lg bg-emerald-50 py-2">
                      <p className="text-lg font-bold text-emerald-700">{dados.confirmacaoPresenca.totalConfirmados}</p>
                      <p className="text-[10px] text-emerald-700 uppercase tracking-wide">Confirmados</p>
                    </div>
                    <div className="rounded-lg bg-red-50 py-2">
                      <p className="text-lg font-bold text-red-600">{dados.confirmacaoPresenca.totalCancelados}</p>
                      <p className="text-[10px] text-red-600 uppercase tracking-wide">Cancelados</p>
                    </div>
                    <div className="rounded-lg bg-gray-50 py-2">
                      <p className="text-lg font-bold text-gray-600">{dados.confirmacaoPresenca.totalPendentes}</p>
                      <p className="text-[10px] text-gray-500 uppercase tracking-wide">Pendentes</p>
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>

          {dados.tempoMedioEsperaPorEspecialidade.length > 0 && (
            <div className="rounded-2xl border bg-white shadow-sm p-6 mb-8">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-semibold text-gray-700">Tempo Médio de Espera por Especialidade</h2>
                <span className="text-xs text-gray-400">Em dias</span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="text-left text-gray-400 uppercase tracking-wide">
                      <th className="pb-2 font-medium">Especialidade</th>
                      <th className="pb-2 font-medium text-right">Backlog atual (aguardando)</th>
                      <th className="pb-2 font-medium text-right">Até a conclusão (atendidos)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dados.tempoMedioEsperaPorEspecialidade.map((item) => (
                      <tr key={item.especialidade} className="border-t">
                        <td className="py-2 font-medium text-gray-700">{item.especialidade}</td>
                        <td className="py-2 text-right text-gray-600">
                          {item.mediaDiasEsperaAtual != null ? `${item.mediaDiasEsperaAtual.toFixed(1)} dias` : '—'}
                        </td>
                        <td className="py-2 text-right text-gray-600">
                          {item.mediaDiasAteConclusao != null ? `${item.mediaDiasAteConclusao.toFixed(1)} dias` : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          <div className="rounded-2xl border bg-white shadow-sm p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-gray-700">Demanda por Especialidade</h2>
            </div>

            {dados.demandaPorEspecialidade.length === 0 ? (
              <p className="text-sm text-gray-400">Nenhum paciente aguardando no momento.</p>
            ) : (
              <div className="space-y-3">
                {dados.demandaPorEspecialidade.map((item) => {
                  const max = dados.demandaPorEspecialidade[0]?.totalAguardando || 1
                  const largura = Math.max(6, (item.totalAguardando / max) * 100)
                  return (
                    <div key={item.especialidade}>
                      <div className="flex justify-between text-xs mb-1">
                        <span className="font-medium text-gray-700">{item.especialidade}</span>
                        <span className="text-gray-400">{item.totalAguardando} aguardando</span>
                      </div>
                      <div className="h-2 rounded-full bg-gray-100 overflow-hidden">
                        <div
                          className="h-full rounded-full bg-brand-teal"
                          style={{ width: `${largura}%` }}
                        />
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </>
      )}
    </AdminLayout>
  )
}
