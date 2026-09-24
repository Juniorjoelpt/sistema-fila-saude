import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import type { IntegracaoConfig, TipoIntegracao } from '../api/types'

const INTEGRACOES: {
  tipo: TipoIntegracao
  nome: string
  descricao: string
  placeholderBaseUrl?: string
  placeholderToken?: string
}[] = [
  {
    tipo: 'ESUS',
    nome: 'e-SUS Atenção Primária',
    descricao: 'Prontuário eletrônico da Atenção Primária — busca de dados de pacientes pelo CNS para evitar redigitação.',
  },
  {
    tipo: 'SISREG',
    nome: 'SISREG III',
    descricao: 'Sistema Nacional de Regulação — envio e consulta de solicitações de regulação.',
  },
  {
    tipo: 'CNES',
    nome: 'CNES',
    descricao: 'Cadastro Nacional de Estabelecimentos de Saúde — busca de dados oficiais de unidades pelo código CNES.',
  },
  {
    tipo: 'WHATSAPP',
    nome: 'WhatsApp',
    descricao:
      'Canal opcional para o lembrete de agendamento (além do e-mail), via WhatsApp Business Platform. Requer conta comercial própria da prefeitura, com template de lembrete já aprovado.',
    placeholderBaseUrl: 'Phone Number ID (WhatsApp Business)',
    placeholderToken: 'Access token da Cloud API',
  },
]

function CardIntegracao({
  tipo,
  nome,
  descricao,
  placeholderBaseUrl,
  placeholderToken,
  config,
  onSalvo,
}: {
  tipo: TipoIntegracao
  nome: string
  descricao: string
  placeholderBaseUrl?: string
  placeholderToken?: string
  config: IntegracaoConfig
  onSalvo: (novo: IntegracaoConfig) => void
}) {
  const [baseUrl, setBaseUrl] = useState(config.baseUrl ?? '')
  const [token, setToken] = useState('')
  const [ativo, setAtivo] = useState(config.ativo)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  async function salvar() {
    setSalvando(true)
    setErro(null)
    try {
      const { data } = await api.put<IntegracaoConfig>(`/api/admin/integracoes/${tipo}`, {
        baseUrl: baseUrl || null,
        token: token || null,
        ativo,
      })
      setToken('')
      onSalvo(data)
    } catch {
      setErro('Não foi possível salvar esta integração.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="rounded-2xl border bg-white shadow-sm p-6">
      <div className="flex items-start justify-between gap-3 mb-1">
        <h2 className="text-sm font-semibold text-gray-900">{nome}</h2>
        <span
          className={`rounded-full px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wide shrink-0 ${
            config.ativo ? 'bg-teal-100 text-teal-800' : 'bg-gray-100 text-gray-500'
          }`}
        >
          {config.ativo ? 'Ativa' : 'Desativada'}
        </span>
      </div>
      <p className="text-xs text-gray-500 mb-4">{descricao}</p>

      <div className="space-y-3">
        <input
          placeholder={placeholderBaseUrl ?? 'URL base da API (quando disponível)'}
          value={baseUrl}
          onChange={(e) => setBaseUrl(e.target.value)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        <input
          placeholder={
            config.tokenConfigurado
              ? 'Token configurado — deixe em branco para manter'
              : (placeholderToken ?? 'Token / credencial de acesso')
          }
          type="password"
          value={token}
          onChange={(e) => setToken(e.target.value)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        <label className="flex items-center gap-2 text-sm text-gray-700">
          <input type="checkbox" checked={ativo} onChange={(e) => setAtivo(e.target.checked)} />
          Ativar esta integração
        </label>

        {erro && <p className="text-xs text-red-600">{erro}</p>}

        <button
          onClick={salvar}
          disabled={salvando}
          className="w-full rounded-lg bg-brand-navy text-white py-2 text-sm font-semibold disabled:opacity-50"
        >
          {salvando ? 'Salvando…' : 'Salvar'}
        </button>

        <p className="text-[11px] text-gray-400">
          {config.atualizadoEm
            ? `Última atualização: ${new Date(config.atualizadoEm).toLocaleString('pt-BR')}${
                config.atualizadoPorNome ? ` — por ${config.atualizadoPorNome}` : ''
              }`
            : 'Ainda não configurada.'}
        </p>
      </div>
    </div>
  )
}

/**
 * Configuração de integrações externas por tenant. e-SUS, SISREG e CNES são
 * obrigatórias no levantamento de requisitos -- os adaptadores já existem no
 * backend (br.com.filasaude.integracao), prontos para serem conectados assim
 * que a prefeitura tiver acesso oficial às APIs, mas nenhuma é chamada
 * automaticamente ainda. WHATSAPP é opcional (melhoria pós-MVP): quando
 * configurada e ativa, o lembrete diário de agendamento (ver
 * LembreteAgendamentoService) sai também por WhatsApp, além do e-mail.
 */
export function Integracoes() {
  const [configs, setConfigs] = useState<IntegracaoConfig[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  function carregar() {
    api
      .get<IntegracaoConfig[]>('/api/admin/integracoes')
      .then((res) => setConfigs(res.data))
      .catch(() => setErro('Não foi possível carregar as integrações.'))
  }
  useEffect(carregar, [])

  function atualizarConfig(novo: IntegracaoConfig) {
    setConfigs((atual) => (atual ? atual.map((c) => (c.tipo === novo.tipo ? novo : c)) : atual))
  }

  return (
    <AdminLayout>
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Integrações</h1>
      <p className="text-sm text-gray-500 mb-6 max-w-2xl">
        Integrações com sistemas externos e canais adicionais de notificação. Nenhuma delas é obrigatória para
        usar o sistema no dia a dia — enquanto não configuradas, tudo continua funcionando normalmente (cadastro
        manual, lembrete só por e-mail). Configure aqui assim que tiver acesso às credenciais de cada uma.
      </p>

      {erro && <p className="text-sm text-red-600 mb-4">{erro}</p>}

      {configs && (
        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-4 gap-6">
          {INTEGRACOES.map((info) => {
            const config = configs.find((c) => c.tipo === info.tipo)
            if (!config) return null
            return (
              <CardIntegracao
                key={info.tipo}
                tipo={info.tipo}
                nome={info.nome}
                descricao={info.descricao}
                placeholderBaseUrl={info.placeholderBaseUrl}
                placeholderToken={info.placeholderToken}
                config={config}
                onSalvo={atualizarConfig}
              />
            )
          })}
        </div>
      )}
    </AdminLayout>
  )
}
