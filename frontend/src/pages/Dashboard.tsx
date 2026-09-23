import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
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
  icone,
  cor,
}: {
  titulo: string
  valor: number | string
  destaque?: string
  icone: string
  cor: number
}) {
  return (
    <div className="rounded-2xl border bg-white shadow-sm p-5">
      <span className={`flex h-9 w-9 items-center justify-center rounded-full text-base mb-3 ${CORES_ICONE[cor % CORES_ICONE.length]}`}>
        {icone}
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
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Painel Administrativo</h1>

      {erro && <p className="text-sm text-red-600 mb-4">{erro}</p>}

      {!dados && !erro && (
        <div className="grid grid-cols-1 sm:grid-cols-5 gap-4 mb-8">
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
          <div className="grid grid-cols-1 sm:grid-cols-5 gap-4 mb-8">
            <Cartao titulo="Fila de Espera" valor={dados.filaDeEspera} destaque="Aguardando" icone="⏳" cor={0} />
            <Cartao titulo="Agendados" valor={dados.agendadosNoMes} destaque="Neste mês" icone="📅" cor={1} />
            <Cartao titulo="Realizados" valor={dados.realizadosNoAno} destaque="Total no ano" icone="✓" cor={2} />
            <Cartao
              titulo="Taxa de Ocupação"
              valor={dados.taxaOcupacaoGeral != null ? `${dados.taxaOcupacaoGeral}%` : '—'}
              destaque={dados.taxaOcupacaoGeral != null ? 'Cotas do mês' : 'Sem cotas cadastradas'}
              icone="◔"
              cor={3}
            />
            <Cartao
              titulo="Pico Máximo"
              valor={dados.picoOcupacaoGeral != null ? `${dados.picoOcupacaoGeral}%` : '—'}
              destaque="Maior ocupação já registrada"
              icone="📈"
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
