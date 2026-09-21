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
