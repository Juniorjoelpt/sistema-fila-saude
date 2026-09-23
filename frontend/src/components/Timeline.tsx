import type { EtapaPublica } from '../api/types'

const ESTILO_POR_STATUS: Record<string, { circulo: string; texto: string; linha: string }> = {
  REALIZADO: {
    circulo: 'bg-brand-teal border-brand-teal text-white',
    texto: 'text-gray-700 font-medium',
    linha: 'bg-brand-teal',
  },
  EM_ANDAMENTO: {
    circulo: 'bg-white border-brand-navy text-brand-navy ring-4 ring-brand-navy/10',
    texto: 'text-brand-navy font-semibold',
    linha: 'bg-gray-200',
  },
  AGUARDANDO: {
    circulo: 'bg-white border-gray-300 text-gray-400',
    texto: 'text-gray-400',
    linha: 'bg-gray-200',
  },
}

const ROTULO: Record<string, string> = {
  REALIZADO: 'Realizado',
  EM_ANDAMENTO: 'Em andamento',
  AGUARDANDO: 'Aguardando',
}

/**
 * Timeline horizontal do ciclo de atendimento (item 3.1: "ex., ciclo cirúrgico:
 * Consulta Pré-operatória → Risco Cirúrgico → ... → Cirurgia Agendada"), com
 * círculos numerados conectados por uma linha de progresso -- no estilo das
 * telas de referência analisadas. A etapa "em andamento" ganha destaque, e um
 * resumo dela aparece abaixo como um status atual de "aguardando vez".
 */
export function Timeline({ etapas }: { etapas: EtapaPublica[] }) {
  const etapaAtual = etapas.find((e) => e.status === 'EM_ANDAMENTO')

  return (
    <div>
      <div className="overflow-x-auto pb-1 -mx-1 px-1">
        <ol className="flex items-start min-w-max">
          {etapas.map((etapa, idx) => {
            const estilo = ESTILO_POR_STATUS[etapa.status] ?? ESTILO_POR_STATUS.AGUARDANDO
            const ehUltimo = idx === etapas.length - 1
            return (
              <li key={idx} className="flex items-center">
                <div className="flex flex-col items-center w-[88px] text-center">
                  <span
                    className={`flex h-9 w-9 items-center justify-center rounded-full border-2 text-xs font-bold shrink-0 ${estilo.circulo}`}
                  >
                    {etapa.status === 'REALIZADO' ? '✓' : idx + 1}
                  </span>
                  <p className={`mt-2 text-[11px] leading-tight ${estilo.texto}`}>{etapa.nomeEtapa}</p>
                  {etapa.dataRealizacao && (
                    <p className="text-[10px] text-gray-400 mt-0.5">
                      {new Date(etapa.dataRealizacao).toLocaleDateString('pt-BR')}
                    </p>
                  )}
                </div>
                {!ehUltimo && <div className={`h-0.5 w-6 sm:w-10 mt-[18px] shrink-0 ${estilo.linha}`} />}
              </li>
            )
          })}
        </ol>
      </div>

      {etapaAtual && (
        <div className="mt-4 flex items-center justify-between gap-3 rounded-xl bg-brand-teal-light border border-brand-teal/20 px-4 py-3">
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-wide text-brand-teal">Aguardando vez</p>
            <p className="text-sm font-semibold text-gray-900">{etapaAtual.nomeEtapa}</p>
          </div>
          <span className="rounded-full bg-white text-brand-teal px-3 py-1 text-[10px] font-bold uppercase tracking-wide shrink-0">
            {ROTULO[etapaAtual.status]}
          </span>
        </div>
      )}
    </div>
  )
}
