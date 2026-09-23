import type { StatusProtocolo } from '../api/types'

const ROTULOS: Record<StatusProtocolo, string> = {
  AGUARDANDO: 'Aguardando',
  AGENDADO: 'Agendado',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
}

const CORES: Record<StatusProtocolo, string> = {
  AGUARDANDO: 'bg-amber-100 text-amber-800',
  AGENDADO: 'bg-blue-100 text-blue-800',
  EM_ANDAMENTO: 'bg-teal-100 text-teal-800',
  CONCLUIDO: 'bg-green-100 text-green-800',
  CANCELADO: 'bg-red-100 text-red-800',
}

export function StatusBadge({ status }: { status: StatusProtocolo }) {
  return (
    <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${CORES[status]}`}>
      {ROTULOS[status]}
    </span>
  )
}

/**
 * Banner de status em destaque para o módulo do cidadão (item 3.1) -- card sólido colorido
 * com ícone, no estilo do "AGENDADO"/"status atual" das telas de referência analisadas.
 */
const BANNER_ESTILO: Record<StatusProtocolo, { bg: string; icone: string }> = {
  AGUARDANDO: { bg: 'bg-status-aguardando', icone: '⏳' },
  AGENDADO: { bg: 'bg-status-agendado', icone: '✓' },
  EM_ANDAMENTO: { bg: 'bg-status-andamento', icone: '⚑' },
  CONCLUIDO: { bg: 'bg-status-concluido', icone: '✓' },
  CANCELADO: { bg: 'bg-status-cancelado', icone: '✕' },
}

export function StatusBanner({ status }: { status: StatusProtocolo }) {
  const estilo = BANNER_ESTILO[status]
  return (
    <div className={`rounded-2xl ${estilo.bg} text-white px-5 py-4 flex items-center gap-3`}>
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/20 text-base">
        {estilo.icone}
      </span>
      <div>
        <p className="text-[10px] font-semibold uppercase tracking-wide text-white/80">Status atual</p>
        <p className="text-lg font-extrabold leading-tight">{ROTULOS[status]}</p>
      </div>
    </div>
  )
}

const ROTULOS_CATEGORIA: Record<string, string> = {
  URGENCIA: 'Urgência',
  JUDICIAL: 'Judicial',
  ESPECIAL: 'Especial (80+)',
  LEGAL: 'Legal',
  NORMAL: 'Normal',
}

const CORES_CATEGORIA: Record<string, string> = {
  URGENCIA: 'bg-red-100 text-red-800',
  JUDICIAL: 'bg-amber-100 text-amber-900',
  ESPECIAL: 'bg-emerald-100 text-emerald-800',
  LEGAL: 'bg-sky-100 text-sky-800',
  NORMAL: 'bg-gray-100 text-gray-700',
}

export function CategoriaBadge({ categoria }: { categoria: string }) {
  return (
    <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${CORES_CATEGORIA[categoria] ?? 'bg-gray-100 text-gray-700'}`}>
      {ROTULOS_CATEGORIA[categoria] ?? categoria}
    </span>
  )
}

/** Badge de prazo (No Prazo / Atenção / Atrasado) -- item 3.2, listagem da fila. */
export type NivelPrazo = 'no-prazo' | 'atencao' | 'atrasado'

const PRAZO_ESTILO: Record<NivelPrazo, { rotulo: string; cor: string }> = {
  'no-prazo': { rotulo: 'No Prazo', cor: 'bg-green-100 text-green-800' },
  atencao: { rotulo: 'Atenção', cor: 'bg-amber-100 text-amber-800' },
  atrasado: { rotulo: 'Atrasado', cor: 'bg-red-100 text-red-800' },
}

export function PrazoBadge({ nivel }: { nivel: NivelPrazo }) {
  const estilo = PRAZO_ESTILO[nivel]
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-semibold ${estilo.cor}`}>
      {estilo.rotulo}
    </span>
  )
}
