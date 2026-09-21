import type { EtapaPublica } from '../api/types'

const ESTILO_POR_STATUS: Record<string, { ponto: string; texto: string; tag: string }> = {
  REALIZADO: {
    ponto: 'bg-brand-teal border-brand-teal',
    texto: 'text-gray-900',
    tag: 'bg-teal-100 text-teal-800',
  },
  EM_ANDAMENTO: {
    ponto: 'bg-brand-navy border-brand-navy',
    texto: 'text-gray-900 font-semibold',
    tag: 'bg-blue-100 text-blue-800',
  },
  AGUARDANDO: {
    ponto: 'bg-white border-gray-300',
    texto: 'text-gray-400',
    tag: 'bg-gray-100 text-gray-500',
  },
}

const ROTULO: Record<string, string> = {
  REALIZADO: 'Realizado',
  EM_ANDAMENTO: 'Em andamento',
  AGUARDANDO: 'Aguardando',
}

export function Timeline({ etapas }: { etapas: EtapaPublica[] }) {
  return (
    <ol className="relative border-l-2 border-gray-200 ml-3">
      {etapas.map((etapa, idx) => {
        const estilo = ESTILO_POR_STATUS[etapa.status] ?? ESTILO_POR_STATUS.AGUARDANDO
        return (
          <li key={idx} className="mb-6 ml-6 last:mb-0">
            <span
              className={`absolute -left-[9px] flex h-4 w-4 items-center justify-center rounded-full border-2 ${estilo.ponto}`}
            />
            <div className="flex flex-wrap items-center gap-2">
              <p className={`text-sm ${estilo.texto}`}>{etapa.nomeEtapa}</p>
              <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${estilo.tag}`}>
                {ROTULO[etapa.status] ?? etapa.status}
              </span>
            </div>
            {etapa.dataRealizacao && (
              <p className="text-xs text-gray-400 mt-0.5">
                {new Date(etapa.dataRealizacao).toLocaleDateString('pt-BR')}
              </p>
            )}
          </li>
        )
      })}
    </ol>
  )
}
