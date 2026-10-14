import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { Spinner } from '../components/Spinner'
import { useTenantBranding } from '../context/TenantBrandingContext'
import type { ContagemPublica, Transparencia as TransparenciaDados } from '../api/types'
import logoSimbolo from '../assets/brand/agenda-saude-simbolo.svg'

/** "2026-09" -> "set/2026" */
function rotuloMes(mes: string): string {
  const [ano, m] = mes.split('-')
  const nomes = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']
  return `${nomes[Number(m) - 1] ?? m}/${ano}`
}

function formatarDataHora(iso: string): string {
  const d = new Date(iso)
  return Number.isNaN(d.getTime())
    ? iso
    : d.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

function Cartao({ titulo, valor, detalhe }: { titulo: string; valor: string; detalhe?: string }) {
  return (
    <div className="rounded-2xl border bg-white shadow-sm p-5">
      <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">{titulo}</p>
      <p className="text-3xl font-extrabold text-brand-navy mt-1">{valor}</p>
      {detalhe && <p className="text-xs text-gray-500 mt-1">{detalhe}</p>}
    </div>
  )
}

function Secao({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <section className="mb-10">
      <h2 className="text-lg font-bold text-brand-navy mb-3">{titulo}</h2>
      {children}
    </section>
  )
}

/**
 * Transparência ativa (LAI): indicadores AGREGADOS da fila de regulação, sem login.
 * Nenhum dado de paciente é exibido; grupos pequenos aparecem como "menos de N".
 * `?embed=1` remove o cabeçalho/rodapé para uso em iframe no portal da prefeitura.
 */
export function Transparencia() {
  const branding = useTenantBranding()
  const embed = new URLSearchParams(window.location.search).get('embed') === '1'
  const [dados, setDados] = useState<TransparenciaDados | null>(null)
  const [naoPublicada, setNaoPublicada] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [baixando, setBaixando] = useState<'csv' | 'json' | null>(null)

  useEffect(() => {
    api
      .get<TransparenciaDados>('/api/public/transparencia')
      .then((res) => setDados(res.data))
      .catch((err) => {
        if (err?.response?.status === 404) {
          setNaoPublicada(true)
        } else {
          setErro('Não foi possível carregar os indicadores agora. Tente novamente em instantes.')
        }
      })
      .finally(() => setCarregando(false))
  }, [])

  async function baixar(formato: 'csv' | 'json') {
    setBaixando(formato)
    try {
      const res = await api.get('/api/public/transparencia/export', {
        params: { formato },
        responseType: 'blob',
      })
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `transparencia-fila.${formato}`)
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch {
      setErro('Não foi possível baixar o arquivo agora.')
    } finally {
      setBaixando(null)
    }
  }

  const maxMes = Math.max(1, ...(dados?.atendimentos ?? []).map((a) => a.concluidos.valor ?? 0))

  return (
    <div className={embed ? 'bg-transparent' : 'min-h-screen flex flex-col bg-gray-50'}>
      {!embed && (
        <header className="border-b bg-white">
          <div className="max-w-5xl mx-auto px-6 py-4 flex items-center gap-2">
            {branding.logoUrl ? (
              <img src={branding.logoUrl} alt={branding.nomeMunicipio ?? 'Logo'} className="h-8 max-w-[160px] object-contain" />
            ) : (
              <img src={logoSimbolo} alt="Agenda Saúde" className="h-8 w-8" />
            )}
            <span className="font-bold text-lg text-brand-navy">
              Transparência da fila de saúde{branding.nomeMunicipio ? ` · ${branding.nomeMunicipio}` : ''}
            </span>
          </div>
        </header>
      )}

      <main className={`max-w-5xl mx-auto w-full px-6 ${embed ? 'py-4' : 'py-10 flex-1'}`}>
        {carregando && (
          <div className="py-16 flex justify-center">
            <Spinner />
          </div>
        )}

        {naoPublicada && (
          <p className="rounded-xl border bg-white px-5 py-6 text-sm text-gray-600">
            Esta prefeitura ainda não publicou os indicadores de transparência da fila de saúde.
          </p>
        )}

        {erro && <p className="mb-4 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3">{erro}</p>}

        {dados && (
          <>
            {dados.textoIntroducao && (
              <p className="text-gray-600 mb-8 whitespace-pre-line">{dados.textoIntroducao}</p>
            )}

            {dados.fila && (
              <Secao titulo="Fila de espera hoje">
                <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
                  <Cartao titulo="Aguardando atendimento" valor={dados.fila.totalAguardando.rotulo} />
                  <Cartao
                    titulo="Dentro do prazo"
                    valor={dados.fila.percentualDentroDoPrazo != null ? `${dados.fila.percentualDentroDoPrazo}%` : '—'}
                  />
                  <Cartao
                    titulo="Em atenção"
                    valor={dados.fila.percentualEmAtencao != null ? `${dados.fila.percentualEmAtencao}%` : '—'}
                  />
                  <Cartao
                    titulo="Fora do prazo"
                    valor={dados.fila.percentualForaDoPrazo != null ? `${dados.fila.percentualForaDoPrazo}%` : '—'}
                  />
                </div>
                <div className="rounded-2xl border bg-white shadow-sm overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="text-left text-xs uppercase text-gray-500 border-b">
                      <tr>
                        <th className="px-4 py-3">Procedimento</th>
                        <th className="px-4 py-3">Aguardando</th>
                        <th className="px-4 py-3">Espera média</th>
                        <th className="px-4 py-3">Espera máxima</th>
                        <th className="px-4 py-3">Estimativa p/ esvaziar</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dados.fila.porProcedimento.length === 0 && (
                        <tr>
                          <td colSpan={5} className="px-4 py-5 text-gray-500">
                            Nenhuma pessoa aguardando no momento.
                          </td>
                        </tr>
                      )}
                      {dados.fila.porProcedimento.map((p) => (
                        <tr key={p.procedimento} className="border-b last:border-0">
                          <td className="px-4 py-3">
                            <p className="font-medium text-gray-900">{p.procedimento}</p>
                            {p.especialidade && <p className="text-xs text-gray-500">{p.especialidade}</p>}
                          </td>
                          <td className="px-4 py-3">{p.aguardando.rotulo}</td>
                          <td className="px-4 py-3">{p.esperaMediaDias != null ? `${p.esperaMediaDias} dias` : '—'}</td>
                          <td className="px-4 py-3">{p.esperaMaximaDias != null ? `${p.esperaMaximaDias} dias` : '—'}</td>
                          <td className="px-4 py-3">
                            {p.diasParaEsvaziarFila != null ? `${p.diasParaEsvaziarFila} dias` : '—'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <p className="text-xs text-gray-500 mt-2">
                  Estimativa baseada no ritmo de atendimento dos últimos 90 dias. "—" significa grupo pequeno demais
                  para divulgar ou ainda sem histórico suficiente.
                </p>
              </Secao>
            )}

            {dados.atendimentos && (
              <Secao titulo="Atendimentos concluídos por mês">
                <div className="rounded-2xl border bg-white shadow-sm p-5">
                  <div className="flex items-end gap-2 h-40" role="img" aria-label="Atendimentos concluídos por mês">
                    {dados.atendimentos.map((a: { mes: string; concluidos: ContagemPublica }) => {
                      const valor = a.concluidos.valor
                      const altura = valor == null ? 8 : Math.max(2, Math.round((valor / maxMes) * 100))
                      return (
                        <div key={a.mes} className="flex-1 flex flex-col items-center justify-end h-full min-w-0">
                          <span className="text-[10px] text-gray-600 mb-1 text-center leading-tight">
                            {a.concluidos.rotulo}
                          </span>
                          <div
                            className={`w-full rounded-t ${valor == null ? 'bg-gray-300' : 'bg-brand-teal'}`}
                            style={{ height: `${altura}%` }}
                            title={`${rotuloMes(a.mes)}: ${a.concluidos.rotulo}`}
                          />
                        </div>
                      )
                    })}
                  </div>
                  <div className="flex gap-2 mt-2">
                    {dados.atendimentos.map((a) => (
                      <span key={a.mes} className="flex-1 text-[10px] text-gray-500 text-center min-w-0">
                        {rotuloMes(a.mes)}
                      </span>
                    ))}
                  </div>
                </div>
              </Secao>
            )}

            {dados.vagas && (
              <Secao titulo="Vagas ofertadas na agenda">
                <div className="rounded-2xl border bg-white shadow-sm overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="text-left text-xs uppercase text-gray-500 border-b">
                      <tr>
                        <th className="px-4 py-3">Unidade de saúde</th>
                        <th className="px-4 py-3">Vagas ({dados.vagas[0]?.periodo ?? 'próximos 30 dias'})</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dados.vagas.length === 0 && (
                        <tr>
                          <td colSpan={2} className="px-4 py-5 text-gray-500">
                            Nenhum horário cadastrado para os próximos dias.
                          </td>
                        </tr>
                      )}
                      {dados.vagas.map((v) => (
                        <tr key={v.unidade} className="border-b last:border-0">
                          <td className="px-4 py-3">{v.unidade}</td>
                          <td className="px-4 py-3">{v.vagasOfertadas}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Secao>
            )}

            {dados.criterios && (
              <Secao titulo="Como a fila é ordenada">
                <ol className="rounded-2xl border bg-white shadow-sm divide-y">
                  {dados.criterios.map((c) => (
                    <li key={c.ordem} className="px-5 py-3 flex gap-3">
                      <span className="h-6 w-6 shrink-0 rounded-full bg-brand-navy text-white text-xs font-bold flex items-center justify-center">
                        {c.ordem}
                      </span>
                      <span className="text-sm">
                        <strong className="text-gray-900">{c.categoria}.</strong>{' '}
                        <span className="text-gray-600">{c.descricao}</span>
                      </span>
                    </li>
                  ))}
                </ol>
                <p className="text-xs text-gray-500 mt-2">Dentro de cada categoria vale a ordem de chegada.</p>
              </Secao>
            )}

            <Secao titulo="Dados abertos e Lei de Acesso à Informação">
              <div className="rounded-2xl border bg-white shadow-sm p-5 space-y-3">
                <div className="flex flex-wrap gap-2">
                  <button
                    onClick={() => baixar('csv')}
                    disabled={baixando !== null}
                    className="rounded-lg bg-brand-navy text-white px-4 py-2 text-sm font-semibold disabled:opacity-50"
                  >
                    {baixando === 'csv' ? 'Baixando…' : 'Baixar planilha (CSV)'}
                  </button>
                  <button
                    onClick={() => baixar('json')}
                    disabled={baixando !== null}
                    className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700 disabled:opacity-50"
                  >
                    {baixando === 'json' ? 'Baixando…' : 'Baixar dados (JSON)'}
                  </button>
                </div>
                {dados.contatoLai && (
                  <p className="text-sm text-gray-700">
                    <strong>Pedido de informação (LAI):</strong> {dados.contatoLai}
                  </p>
                )}
                <p className="text-xs text-gray-500 leading-relaxed">
                  Atualizado em {formatarDataHora(dados.geradoEm)}. Os números são agregados e não identificam
                  pacientes (LGPD): grupos com menos de {dados.limiteSupressao} pessoas aparecem como "menos de{' '}
                  {dados.limiteSupressao}". Para acompanhar o seu próprio atendimento, use a consulta de protocolo com
                  CPF ou CNS na página inicial.
                </p>
              </div>
            </Secao>
          </>
        )}
      </main>
    </div>
  )
}
