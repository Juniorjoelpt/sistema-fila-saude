import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import type { DashboardResponse } from '../api/types'

function Cartao({ titulo, valor, destaque }: { titulo: string; valor: number | string; destaque?: string }) {
  return (
    <div className="rounded-2xl border bg-white shadow-sm p-5">
      <p className="text-xs text-gray-400 uppercase tracking-wide">{titulo}</p>
      <p className="text-3xl font-extrabold text-brand-navy mt-1">{valor}</p>
      {destaque && <p className="text-xs text-brand-teal font-medium mt-1">{destaque}</p>}
    </div>
  )
}

export function Dashboard() {
  const [dados, setDados] = useState<DashboardResponse | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    api
      .get<DashboardResponse>('/api/admin/dashboard')
      .then((res) => setDados(res.data))
      .catch(() => setErro('Não foi possível carregar os indicadores.'))
  }, [])

  return (
    <AdminLayout>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Painel Administrativo</h1>

      {erro && <p className="text-sm text-red-600 mb-4">{erro}</p>}

      {dados && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
            <Cartao titulo="Fila de Espera" valor={dados.filaDeEspera} destaque="Aguardando" />
            <Cartao titulo="Agendados" valor={dados.agendadosNoMes} destaque="Neste mês" />
            <Cartao titulo="Realizados" valor={dados.realizadosNoAno} destaque="Total no ano" />
          </div>

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
