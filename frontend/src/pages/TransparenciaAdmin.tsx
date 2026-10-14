import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import { Spinner } from '../components/Spinner'
import type { Transparencia, TransparenciaConfig } from '../api/types'

type Secao = 'mostrarFila' | 'mostrarAtendimentos' | 'mostrarVagas' | 'mostrarCriterios'

const SECOES: { chave: Secao; rotulo: string; ajuda: string }[] = [
  { chave: 'mostrarFila', rotulo: 'Fila de espera', ajuda: 'Quantos aguardam, prazos e espera por procedimento.' },
  { chave: 'mostrarAtendimentos', rotulo: 'Atendimentos concluídos', ajuda: 'Concluídos por mês, nos últimos 12 meses.' },
  { chave: 'mostrarVagas', rotulo: 'Vagas ofertadas', ajuda: 'Vagas na agenda por unidade, nos próximos 30 dias.' },
  { chave: 'mostrarCriterios', rotulo: 'Critérios da fila', ajuda: 'Como a fila é ordenada (Urgência, Judicial, 80+...).' },
]

/** Admin: liga/desliga a página pública de transparência ativa (LAI) e escolhe o que ela mostra. */
export function TransparenciaAdmin() {
  const [config, setConfig] = useState<TransparenciaConfig | null>(null)
  const [previa, setPrevia] = useState<Transparencia | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)

  function carregar() {
    setCarregando(true)
    Promise.all([
      api.get<TransparenciaConfig>('/api/admin/transparencia/config'),
      api.get<Transparencia>('/api/admin/transparencia/previa'),
    ])
      .then(([c, p]) => {
        setConfig(c.data)
        setPrevia(p.data)
      })
      .catch(() => setErro('Não foi possível carregar a configuração.'))
      .finally(() => setCarregando(false))
  }

  useEffect(carregar, [])

  async function salvar(proximo: TransparenciaConfig, mensagem: string) {
    setSalvando(true)
    setErro(null)
    setAviso(null)
    try {
      const { data } = await api.put<TransparenciaConfig>('/api/admin/transparencia/config', proximo)
      setConfig(data)
      setAviso(mensagem)
      const p = await api.get<Transparencia>('/api/admin/transparencia/previa')
      setPrevia(p.data)
    } catch (err: any) {
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível salvar.')
    } finally {
      setSalvando(false)
    }
  }

  const origem = window.location.origin
  const tenant = new URLSearchParams(window.location.search).get('tenant')
  const linkPublico = `${origem}/transparencia`
  const trechoIframe = `<iframe src="${origem}/transparencia?embed=1${tenant ? `&tenant=${encodeURIComponent(tenant)}` : ''}" title="Transparência da fila de saúde" width="100%" height="1400" style="border:0"></iframe>`

  if (carregando || !config) {
    return (
      <AdminLayout>
        <div className="py-20 flex justify-center">{erro ? <p className="text-red-600 text-sm">{erro}</p> : <Spinner />}</div>
      </AdminLayout>
    )
  }

  return (
    <AdminLayout>
      <div className="max-w-3xl">
        <h1 className="text-2xl font-bold text-gray-900 mb-1">Transparência ativa</h1>
        <p className="text-sm text-gray-500 mb-6">
          Página pública (sem login) com indicadores agregados da fila, para cumprir a Lei de Acesso à Informação.
          Ela nunca mostra nome, CPF, CNS ou número de protocolo; grupos com menos de 5 pessoas aparecem como
          "menos de 5".
        </p>

        {erro && <p className="text-sm text-red-600 mb-4">{erro}</p>}
        {aviso && <p className="text-sm text-brand-teal font-medium mb-4">{aviso}</p>}

        <section className="rounded-2xl border bg-white shadow-sm p-6 mb-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 className="text-sm font-semibold text-gray-700">Situação</h2>
              <p className="text-sm mt-1">
                {config.publicado ? (
                  <span className="text-emerald-700 font-semibold">Publicada — qualquer pessoa pode acessar.</span>
                ) : (
                  <span className="text-amber-700 font-semibold">Não publicada — o endereço público responde "ainda não publicada".</span>
                )}
              </p>
            </div>
            <button
              onClick={() =>
                salvar(
                  { ...config, publicado: !config.publicado },
                  config.publicado ? 'Página despublicada.' : 'Página publicada.',
                )
              }
              disabled={salvando}
              className={`shrink-0 rounded-lg px-4 py-2 text-sm font-semibold disabled:opacity-50 ${
                config.publicado ? 'border border-gray-300 text-gray-700' : 'btn-primary text-white'
              }`}
            >
              {config.publicado ? 'Despublicar' : 'Publicar página'}
            </button>
          </div>

          {config.publicado && (
            <div className="mt-4 space-y-3 text-sm">
              <div>
                <p className="text-xs font-semibold text-gray-500 uppercase">Endereço público</p>
                <a href={linkPublico} target="_blank" rel="noreferrer" className="text-brand-teal underline break-all">
                  {linkPublico}
                </a>
              </div>
              <div>
                <p className="text-xs font-semibold text-gray-500 uppercase mb-1">Para colar no portal da prefeitura (iframe)</p>
                <textarea
                  readOnly
                  value={trechoIframe}
                  rows={3}
                  onFocus={(e) => e.currentTarget.select()}
                  className="w-full rounded-lg border border-gray-300 bg-gray-50 px-3 py-2 text-xs font-mono"
                />
              </div>
            </div>
          )}
        </section>

        <section className="rounded-2xl border bg-white shadow-sm p-6 mb-6">
          <h2 className="text-sm font-semibold text-gray-700 mb-3">O que mostrar</h2>
          <div className="space-y-3">
            {SECOES.map((s) => (
              <label key={s.chave} className="flex items-start gap-3 text-sm">
                <input
                  type="checkbox"
                  checked={config[s.chave]}
                  onChange={(e) => setConfig({ ...config, [s.chave]: e.target.checked })}
                  className="mt-1"
                />
                <span>
                  <span className="font-medium text-gray-900">{s.rotulo}</span>
                  <span className="block text-xs text-gray-500">{s.ajuda}</span>
                </span>
              </label>
            ))}
          </div>

          <label className="block mt-5 text-sm font-medium text-gray-700">
            Texto de apresentação (opcional)
            <textarea
              value={config.textoIntroducao ?? ''}
              onChange={(e) => setConfig({ ...config, textoIntroducao: e.target.value })}
              maxLength={1000}
              rows={3}
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-normal"
              placeholder="Ex.: A Secretaria de Saúde divulga mensalmente os números da fila de regulação."
            />
          </label>
          <label className="block mt-4 text-sm font-medium text-gray-700">
            Contato para pedidos de informação (LAI)
            <input
              value={config.contatoLai ?? ''}
              onChange={(e) => setConfig({ ...config, contatoLai: e.target.value })}
              maxLength={300}
              className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-normal"
              placeholder="Ex.: Ouvidoria — ouvidoria@prefeitura.pi.gov.br / (86) 0000-0000"
            />
          </label>

          <button
            onClick={() => salvar(config, 'Configuração salva.')}
            disabled={salvando}
            className="mt-5 btn-primary rounded-lg text-white px-5 py-2 text-sm font-semibold disabled:opacity-50"
          >
            {salvando ? 'Salvando…' : 'Salvar configuração'}
          </button>
        </section>

        {previa && (
          <section className="rounded-2xl border bg-white shadow-sm p-6">
            <h2 className="text-sm font-semibold text-gray-700 mb-3">Prévia do que seria publicado agora</h2>
            <ul className="text-sm text-gray-700 space-y-1">
              {previa.fila && (
                <li>
                  Fila: {previa.fila.totalAguardando.rotulo} aguardando, {previa.fila.porProcedimento.length} procedimento(s) listado(s).
                </li>
              )}
              {previa.atendimentos && <li>Atendimentos: {previa.atendimentos.length} meses.</li>}
              {previa.vagas && <li>Vagas: {previa.vagas.length} unidade(s) com horários nos próximos 30 dias.</li>}
              {previa.criterios && <li>Critérios da fila: {previa.criterios.length} categorias.</li>}
              {!previa.fila && !previa.atendimentos && !previa.vagas && !previa.criterios && (
                <li className="text-amber-700">Nenhuma seção selecionada: a página ficaria vazia.</li>
              )}
            </ul>
          </section>
        )}
      </div>
    </AdminLayout>
  )
}
