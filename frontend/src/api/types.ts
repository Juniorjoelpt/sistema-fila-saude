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
  nomeUnidadeSaude: string | null
  status: StatusProtocolo
  posicaoFila: number | null
  dataInclusao: string
  dataPrevista: string | null
  horaAgendada: string | null
  etapas: EtapaPublica[]
}

export interface LoginResponse {
  requerDoisFatores: boolean
  loginToken: string | null
  token: string | null
  nome: string | null
  email: string | null
  papel: Papel | null
}

export interface TwoFactorStatus {
  habilitado: boolean
}

export interface TwoFactorSetup {
  secretBase32: string
  qrCodeBase64Png: string
}

export interface Protocolo {
  id: number
  numeroProtocolo: string
  nomePaciente: string
  procedimentoId: number
  nomeProcedimento: string
  especialidadeProcedimento: string | null
  unidadeSaudeId: number | null
  nomeUnidadeSaude: string | null
  categoriaPrioridade: CategoriaPrioridade
  status: StatusProtocolo
  processoJudicial: string | null
  dataSolicitacao: string
  dataInclusao: string
  dataPrevista: string | null
  horaAgendada: string | null
  diasEmEspera: number
  posicaoFila: number | null
}

export interface HorarioAgenda {
  id: number
  unidadeSaudeId: number
  nomeUnidadeSaude: string
  especialidade: string
  data: string
  horaInicio: string
  capacidadeTotal: number
  vagasOcupadas: number
  vagasDisponiveis: number
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface OcupacaoEspecialidade {
  especialidade: string
  quantidadeTotal: number
  quantidadeUtilizada: number
  percentual: number
  gargalo: boolean
}

export interface DashboardResponse {
  filaDeEspera: number
  agendadosNoMes: number
  realizadosNoAno: number
  demandaPorEspecialidade: { especialidade: string; totalAguardando: number }[]
  taxaOcupacaoGeral: number | null
  picoOcupacaoGeral: number | null
  ocupacaoPorEspecialidade: OcupacaoEspecialidade[]
}

export interface Paciente {
  id: number
  nome: string
  cpf: string | null
  cns: string | null
  dataNascimento: string | null
  telefone: string | null
  email: string | null
  acsResponsavelId: number | null
  acsResponsavelNome: string | null
  pcd: boolean
  gestante: boolean
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
  codigoCnes: string | null
  ativo: boolean
}

export interface TenantBranding {
  nomeMunicipio: string | null
  corPrimaria: string | null
  corSecundaria: string | null
  logoUrl: string | null
}

export type TipoIntegracao = 'ESUS' | 'SISREG' | 'CNES' | 'WHATSAPP'

export interface IntegracaoConfig {
  tipo: TipoIntegracao
  baseUrl: string | null
  tokenConfigurado: boolean
  ativo: boolean
  atualizadoEm: string | null
  atualizadoPorNome: string | null
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

export interface HistoricoPrioridadeItem {
  prioridadeAnterior: CategoriaPrioridade
  prioridadeNova: CategoriaPrioridade
  usuarioNome: string | null
  motivo: string
  criadoEm: string
}

export interface ProtocoloDetalhe extends Protocolo {
  etapas: EtapaAdmin[]
  historico: HistoricoStatusItem[]
  historicoPrioridade: HistoricoPrioridadeItem[]
}

export interface Usuario {
  id: number
  nome: string
  email: string
  papel: Papel
  ativo: boolean
}

export interface Cota {
  id: number
  unidadeSaudeId: number
  nomeUnidadeSaude: string
  especialidade: string
  mesReferencia: string // "YYYY-MM"
  quantidadeTotal: number
  quantidadeUtilizada: number
  percentualPreenchido: number
}

export interface ImportacaoErro {
  linha: number
  motivo: string
}

export interface ImportacaoResultado {
  totalLinhas: number
  importados: number
  erros: ImportacaoErro[]
}

export interface LogAuditoria {
  id: number
  usuarioNome: string | null
  usuarioEmail: string | null
  acao: string
  entidade: string | null
  entidadeId: number | null
  detalhe: string | null
  criadoEm: string
}

export interface ConfirmacaoPresenca {
  numeroProtocolo: string
  nomePaciente: string
  nomeProcedimento: string
  nomeUnidadeSaude: string | null
  dataPrevista: string | null
  horaAgendada: string | null
  presencaConfirmacao: 'PENDENTE' | 'CONFIRMADA' | 'CANCELADA' | null
}

export interface CotaAjuste {
  quantidadeAnterior: number
  quantidadeNova: number
  usuarioNome: string
  motivo: string | null
  criadoEm: string
}
