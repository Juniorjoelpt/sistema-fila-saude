export type StatusProtocolo = 'AGUARDANDO' | 'AGENDADO' | 'EM_ANDAMENTO' | 'CONCLUIDO' | 'CANCELADO'
export type StatusEtapa = 'REALIZADO' | 'EM_ANDAMENTO' | 'AGUARDANDO'
export type CategoriaPrioridade = 'URGENCIA' | 'JUDICIAL' | 'ESPECIAL' | 'LEGAL' | 'NORMAL'
export type Papel = 'ACS' | 'REGULADOR' | 'ADMIN'

export interface EtapaPublica {
  nomeEtapa: string
  ordem: number
  status: StatusEtapa
  dataRealizacao: string | null
}

export interface ProtocoloPublico {
  numeroProtocolo: string
  nomePaciente: string
  nomeProcedimento: string
  status: StatusProtocolo
  posicaoFila: number | null
  dataInclusao: string
  dataPrevista: string | null
  etapas: EtapaPublica[]
}

export interface LoginResponse {
  token: string
  nome: string
  email: string
  papel: Papel
}

export interface Protocolo {
  id: number
  numeroProtocolo: string
  nomePaciente: string
  nomeProcedimento: string
  nomeUnidadeSaude: string | null
  categoriaPrioridade: CategoriaPrioridade
  status: StatusProtocolo
  processoJudicial: string | null
  dataSolicitacao: string
  dataInclusao: string
  dataPrevista: string | null
  diasEmEspera: number
  posicaoFila: number | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface DashboardResponse {
  filaDeEspera: number
  agendadosNoMes: number
  realizadosNoAno: number
  demandaPorEspecialidade: { especialidade: string; totalAguardando: number }[]
}

export interface Paciente {
  id: number
  nome: string
  cpf: string | null
  cns: string | null
  dataNascimento: string | null
  telefone: string | null
  email: string | null
  acsResponsavelNome: string | null
}

export interface Procedimento {
  id: number
  nome: string
  tipo: 'CONSULTA' | 'EXAME' | 'CIRURGIA'
  especialidade: string | null
  ativo: boolean
}

export interface UnidadeSaude {
  id: number
  nome: string
  endereco: string | null
  latitude: number | null
  longitude: number | null
  ativo: boolean
}

export interface EtapaAdmin {
  id: number
  nomeEtapa: string
  ordem: number
  status: StatusEtapa
  dataRealizacao: string | null
}

export interface HistoricoStatusItem {
  statusAnterior: StatusProtocolo | null
  statusNovo: StatusProtocolo
  observacao: string | null
  criadoEm: string
}

export interface ProtocoloDetalhe extends Protocolo {
  etapas: EtapaAdmin[]
  historico: HistoricoStatusItem[]
}
