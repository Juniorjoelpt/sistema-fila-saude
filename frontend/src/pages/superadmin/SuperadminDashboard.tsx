import { useEffect, useRef, useState, type FormEvent } from 'react'
import { superadminApi } from '../../api/superadminClient'
import { resolverUrlAsset } from '../../api/client'
import { useSuperadminAuth } from '../../context/SuperadminAuthContext'

const ehUrlExterna = (url: string) => /^https?:\/\//i.test(url)

interface TenantResumo {
  id: number
  slug: string
  nomeMunicipio: string
  ativo: boolean
  corPrimaria: string | null
  corSecundaria: string | null
  logoUrl: string | null
}

interface TenantMetricas {
  slug: string
  totalUsuarios: number
  totalPacientes: number
  totalProtocolos: number
  protocolosAguardando: number
}

interface TenantProvisionado {
  slug: string
  nomeMunicipio: string
  adminEmail: string
  senhaProvisoria: string
}

function LinhaTenant({ tenant, onAtualizado }: { tenant: TenantResumo; onAtualizado: () => void }) {
  const [metricas, setMetricas] = useState<TenantMetricas | null>(null)
  const [carregandoMetricas, setCarregandoMetricas] = useState(false)
  const [alterando, setAlterando] = useState(false)
  const [editandoBranding, setEditandoBranding] = useState(false)
  const [corPrimaria, setCorPrimaria] = useState(tenant.corPrimaria ?? '')
  const [corSecundaria, setCorSecundaria] = useState(tenant.corSecundaria ?? '')
  const [logoUrl, setLogoUrl] = useState(tenant.logoUrl && ehUrlExterna(tenant.logoUrl) ? tenant.logoUrl : '')
  const [salvandoBranding, setSalvandoBranding] = useState(false)
  const [enviandoLogo, setEnviandoLogo] = useState(false)
  const [erroLogo, setErroLogo] = useState<string | null>(null)
  const inputArquivoRef = useRef<HTMLInputElement>(null)
  const temLogoUpload = !!tenant.logoUrl && !ehUrlExterna(tenant.logoUrl)

  async function verMetricas() {
    if (metricas) {
      setMetricas(null)
      return
    }
    setCarregandoMetricas(true)
    try {
      const { data } = await superadminApi.get<TenantMetricas>(`/api/superadmin/tenants/${tenant.slug}/metricas`)
      setMetricas(data)
    } finally {
      setCarregandoMetricas(false)
    }
  }

  async function alternarStatus() {
    setAlterando(true)
    try {
      await superadminApi.patch(`/api/superadmin/tenants/${tenant.slug}/status`, { ativo: !tenant.ativo })
      onAtualizado()
    } finally {
      setAlterando(false)
    }
  }

  async function salvarBranding() {
    setSalvandoBranding(true)
    try {
      await superadminApi.patch(`/api/superadmin/tenants/${tenant.slug}/branding`, {
        corPrimaria: corPrimaria || null,
        corSecundaria: corSecundaria || null,
        logoUrl: logoUrl || null,
      })
      setEditandoBranding(false)
      onAtualizado()
    } finally {
      setSalvandoBranding(false)
    }
  }

  async function enviarLogo(arquivo: File) {
    setErroLogo(null)
    setEnviandoLogo(true)
    try {
      const formData = new FormData()
      formData.append('arquivo', arquivo)
      // Sem header de Content-Type manual: o navegador define automaticamente
      // "multipart/form-data" com o boundary correto ao enviar um FormData.
      await superadminApi.post(`/api/superadmin/tenants/${tenant.slug}/logo`, formData)
      onAtualizado()
    } catch (err: any) {
      setErroLogo(err?.response?.data?.mensagem ?? 'Não foi possível enviar o logo.')
    } finally {
      setEnviandoLogo(false)
      if (inputArquivoRef.current) inputArquivoRef.current.value = ''
    }
  }

  async function removerLogo() {
    setErroLogo(null)
    setEnviandoLogo(true)
    try {
      await superadminApi.delete(`/api/superadmin/tenants/${tenant.slug}/logo`)
      onAtualizado()
    } finally {
      setEnviandoLogo(false)
    }
  }

  return (
    <>
      <tr className="border-b last:border-0">
        <td className="px-4 py-3">
          <div className="flex items-center gap-2">
            {tenant.logoUrl && (
              <img
                src={resolverUrlAsset(tenant.logoUrl) ?? undefined}
                alt={tenant.nomeMunicipio}
                className="h-6 w-6 rounded object-contain"
              />
            )}
            <div>
              <p className="font-semibold text-gray-900">{tenant.nomeMunicipio}</p>
              <p className="text-xs text-gray-400 font-mono">{tenant.slug}</p>
            </div>
          </div>
        </td>
        <td className="px-4 py-3">
          <span
            className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
              tenant.ativo ? 'bg-teal-100 text-teal-800' : 'bg-gray-100 text-gray-500'
            }`}
          >
            {tenant.ativo ? 'Ativo' : 'Inativo'}
          </span>
        </td>
        <td className="px-4 py-3 text-xs">
          <button onClick={verMetricas} className="text-gray-700 font-semibold hover:underline">
            {carregandoMetricas ? 'Carregando…' : metricas ? 'Ocultar uso' : 'Ver uso'}
          </button>
          {metricas && (
            <div className="mt-2 text-gray-500 space-y-0.5">
              <p>{metricas.totalUsuarios} usuário(s) de equipe</p>
              <p>{metricas.totalPacientes} paciente(s) cadastrados</p>
              <p>
                {metricas.totalProtocolos} protocolo(s) ({metricas.protocolosAguardando} aguardando)
              </p>
            </div>
          )}
        </td>
        <td className="px-4 py-3 text-right space-x-2 whitespace-nowrap">
          <button
            onClick={() => setEditandoBranding((atual) => !atual)}
            className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-50"
          >
            {editandoBranding ? 'Cancelar' : 'Identidade visual'}
          </button>
          <button
            onClick={alternarStatus}
            disabled={alterando}
            className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-40"
          >
            {tenant.ativo ? 'Desativar' : 'Ativar'}
          </button>
        </td>
      </tr>
      {editandoBranding && (
        <tr className="border-b last:border-0 bg-gray-50">
          <td colSpan={4} className="px-4 py-4">
            <p className="text-xs font-semibold text-gray-600 mb-2">
              Logo e cores da Secretaria -- aplicadas na tela pública de consulta e no painel administrativo desta
              prefeitura.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <label className="text-xs text-gray-500 space-y-1">
                <span>Cor primária</span>
                <div className="flex gap-2">
                  <input
                    type="color"
                    value={/^#[0-9a-fA-F]{6}$/.test(corPrimaria) ? corPrimaria : '#1f3864'}
                    onChange={(e) => setCorPrimaria(e.target.value)}
                    className="h-9 w-10 shrink-0 rounded-lg border border-gray-300"
                  />
                  <input
                    type="text"
                    placeholder="#1f3864"
                    value={corPrimaria}
                    maxLength={30}
                    onChange={(e) => setCorPrimaria(e.target.value.trim())}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono"
                  />
                </div>
              </label>
              <label className="text-xs text-gray-500 space-y-1">
                <span>Cor secundária</span>
                <div className="flex gap-2">
                  <input
                    type="color"
                    value={/^#[0-9a-fA-F]{6}$/.test(corSecundaria) ? corSecundaria : '#1b7a6e'}
                    onChange={(e) => setCorSecundaria(e.target.value)}
                    className="h-9 w-10 shrink-0 rounded-lg border border-gray-300"
                  />
                  <input
                    type="text"
                    placeholder="#1b7a6e"
                    value={corSecundaria}
                    maxLength={30}
                    onChange={(e) => setCorSecundaria(e.target.value.trim())}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono"
                  />
                </div>
              </label>
              <label className="text-xs text-gray-500 space-y-1">
                <span>URL do logo (alternativa ao upload, se hospedado externamente)</span>
                <input
                  type="text"
                  placeholder="https://…/logo.png"
                  value={logoUrl}
                  onChange={(e) => setLogoUrl(e.target.value)}
                  disabled={temLogoUpload}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm disabled:bg-gray-100 disabled:text-gray-400"
                />
              </label>
            </div>
            <button
              onClick={salvarBranding}
              disabled={salvandoBranding}
              className="mt-3 rounded-lg bg-gray-900 text-white px-4 py-1.5 text-xs font-semibold disabled:opacity-40"
            >
              {salvandoBranding ? 'Salvando…' : 'Salvar identidade visual'}
            </button>

            <div className="mt-4 pt-4 border-t border-gray-200">
              <p className="text-xs font-semibold text-gray-600 mb-2">Upload direto do logo (PNG, até 5MB)</p>
              <div className="flex items-center gap-3">
                {temLogoUpload && (
                  <img
                    src={resolverUrlAsset(tenant.logoUrl) ?? undefined}
                    alt={tenant.nomeMunicipio}
                    className="h-10 w-10 rounded object-contain border"
                  />
                )}
                <input
                  ref={inputArquivoRef}
                  type="file"
                  accept="image/png"
                  disabled={enviandoLogo}
                  onChange={(e) => {
                    const arquivo = e.target.files?.[0]
                    if (arquivo) enviarLogo(arquivo)
                  }}
                  className="text-xs text-gray-600"
                />
                {temLogoUpload && (
                  <button
                    onClick={removerLogo}
                    disabled={enviandoLogo}
                    className="text-xs font-semibold text-red-600 hover:underline disabled:opacity-40"
                  >
                    Remover logo
                  </button>
                )}
                {enviandoLogo && <span className="text-xs text-gray-400">Enviando…</span>}
              </div>
              {erroLogo && <p className="text-xs text-red-600 mt-1">{erroLogo}</p>}
              {temLogoUpload && (
                <p className="text-xs text-gray-400 mt-1">
                  Um logo enviado por upload tem prioridade sobre a URL externa acima.
                </p>
              )}
            </div>
          </td>
        </tr>
      )}
    </>
  )
}

export function SuperadminDashboard() {
  const { usuario, logout } = useSuperadminAuth()
  const [tenants, setTenants] = useState<TenantResumo[]>([])
  const [carregando, setCarregando] = useState(false)

  const [nomeMunicipio, setNomeMunicipio] = useState('')
  const [slug, setSlug] = useState('')
  const [corPrimaria, setCorPrimaria] = useState('')
  const [corSecundaria, setCorSecundaria] = useState('')
  const [logoUrl, setLogoUrl] = useState('')
  const [adminNome, setAdminNome] = useState('')
  const [adminEmail, setAdminEmail] = useState('')
  const [erroForm, setErroForm] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)
  const [provisionado, setProvisionado] = useState<TenantProvisionado | null>(null)

  function carregar() {
    setCarregando(true)
    superadminApi
      .get<TenantResumo[]>('/api/superadmin/tenants')
      .then((res) => setTenants(res.data))
      .finally(() => setCarregando(false))
  }

  useEffect(carregar, [])

  async function provisionarTenant(e: FormEvent) {
    e.preventDefault()
    setErroForm(null)
    if (!nomeMunicipio.trim() || !slug.trim() || !adminNome.trim() || !adminEmail.trim()) {
      setErroForm('Preencha todos os campos obrigatórios.')
      return
    }
    setSalvando(true)
    try {
      const { data } = await superadminApi.post<TenantProvisionado>('/api/superadmin/tenants', {
        nomeMunicipio,
        slug: slug.toLowerCase(),
        corPrimaria: corPrimaria || null,
        corSecundaria: corSecundaria || null,
        logoUrl: logoUrl || null,
        adminNome,
        adminEmail,
      })
      setProvisionado(data)
      setNomeMunicipio('')
      setSlug('')
      setCorPrimaria('')
      setCorSecundaria('')
      setLogoUrl('')
      setAdminNome('')
      setAdminEmail('')
      carregar()
    } catch (err: any) {
      setErroForm(err?.response?.data?.mensagem ?? 'Não foi possível provisionar a prefeitura.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-gray-900 text-white">
        <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
          <div>
            <p className="font-bold">Painel de Superadmin — Fila Saúde</p>
            <p className="text-xs text-white/60">{usuario?.nome}</p>
          </div>
          <button onClick={logout} className="text-xs rounded-lg bg-white/10 hover:bg-white/20 px-3 py-1.5">
            Sair
          </button>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 py-8 animate-fade-in">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Prefeituras clientes</h1>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <form onSubmit={provisionarTenant} className="rounded-2xl border bg-white shadow-sm p-5 space-y-3 h-fit">
            <h2 className="text-sm font-semibold text-gray-700">Provisionar nova prefeitura</h2>
            <input
              placeholder="Nome do município *"
              value={nomeMunicipio}
              onChange={(e) => setNomeMunicipio(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
            <input
              placeholder="Identificador (slug) * — ex.: porto-pi"
              value={slug}
              onChange={(e) => setSlug(e.target.value.toLowerCase())}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono"
            />
            <div className="grid grid-cols-2 gap-2">
              <div className="flex gap-1">
                <input
                  type="color"
                  value={/^#[0-9a-fA-F]{6}$/.test(corPrimaria) ? corPrimaria : '#1f3864'}
                  onChange={(e) => setCorPrimaria(e.target.value)}
                  className="h-9 w-9 shrink-0 rounded-lg border border-gray-300"
                />
                <input
                  placeholder="Cor primária (opcional)"
                  value={corPrimaria}
                  maxLength={30}
                  onChange={(e) => setCorPrimaria(e.target.value.trim())}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono"
                />
              </div>
              <div className="flex gap-1">
                <input
                  type="color"
                  value={/^#[0-9a-fA-F]{6}$/.test(corSecundaria) ? corSecundaria : '#1b7a6e'}
                  onChange={(e) => setCorSecundaria(e.target.value)}
                  className="h-9 w-9 shrink-0 rounded-lg border border-gray-300"
                />
                <input
                  placeholder="Cor secundária (opcional)"
                  value={corSecundaria}
                  maxLength={30}
                  onChange={(e) => setCorSecundaria(e.target.value.trim())}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono"
                />
              </div>
            </div>
            <input
              placeholder="URL do logo (opcional)"
              value={logoUrl}
              onChange={(e) => setLogoUrl(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
            <input
              placeholder="Nome do administrador *"
              value={adminNome}
              onChange={(e) => setAdminNome(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
            <input
              placeholder="E-mail do administrador *"
              type="email"
              value={adminEmail}
              onChange={(e) => setAdminEmail(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
            {erroForm && <p className="text-xs text-red-600">{erroForm}</p>}
            <button
              type="submit"
              disabled={salvando}
              className="w-full rounded-lg bg-gray-900 text-white py-2 text-sm font-semibold disabled:opacity-40"
            >
              {salvando ? 'Provisionando…' : 'Provisionar'}
            </button>
            <p className="text-xs text-gray-400">
              Cria o banco de dados isolado da prefeitura, aplica o schema e gera o primeiro
              usuário administrador.
            </p>
          </form>

          <div className="lg:col-span-2 space-y-4">
            {provisionado && (
              <div className="rounded-2xl border border-teal-200 bg-teal-50 p-5 text-sm">
                <p className="font-semibold text-teal-900 mb-2">
                  Prefeitura "{provisionado.nomeMunicipio}" provisionada com sucesso!
                </p>
                <p className="text-teal-800">
                  Login do admin: <span className="font-mono">{provisionado.adminEmail}</span>
                </p>
                <p className="text-teal-800">
                  Senha provisória: <span className="font-mono font-semibold">{provisionado.senhaProvisoria}</span>
                </p>
                <p className="text-xs text-teal-700 mt-2">
                  Anote esta senha agora — ela não será exibida novamente. Repasse-a à prefeitura por um
                  canal seguro e oriente a troca no primeiro acesso.
                </p>
              </div>
            )}

            <div className="rounded-2xl border bg-white shadow-sm overflow-hidden">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
                  <tr>
                    <th className="text-left px-4 py-3">Prefeitura</th>
                    <th className="text-left px-4 py-3">Situação</th>
                    <th className="text-left px-4 py-3">Uso</th>
                    <th className="text-right px-4 py-3">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {carregando && (
                    <tr>
                      <td colSpan={4} className="px-4 py-8 text-center text-gray-400">
                        Carregando…
                      </td>
                    </tr>
                  )}
                  {!carregando && tenants.length === 0 && (
                    <tr>
                      <td colSpan={4} className="px-4 py-8 text-center text-gray-400">
                        Nenhuma prefeitura provisionada ainda.
                      </td>
                    </tr>
                  )}
                  {!carregando &&
                    tenants.map((t) => <LinhaTenant key={t.id} tenant={t} onAtualizado={carregar} />)}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
