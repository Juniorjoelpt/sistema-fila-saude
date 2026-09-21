import { useState, type FormEvent } from 'react'
import { api } from '../api/client'
import { StatusBadge } from '../components/StatusBadge'
import { Timeline } from '../components/Timeline'
import type { ProtocoloPublico } from '../api/types'

/**
 * Módulo do cidadão (item 3.1): consulta pública de protocolo por CPF ou CNS,
 * sem necessidade de login. Tela de entrada do produto para o munícipe.
 */
export function ConsultaProtocolo() {
  const [documento, setDocumento] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [protocolos, setProtocolos] = useState<ProtocoloPublico[] | null>(null)

  async function buscar(event: FormEvent) {
    event.preventDefault()
    setCarregando(true)
    setErro(null)
    setProtocolos(null)

    try {
      const { data } = await api.get<ProtocoloPublico[]>('/api/public/protocolo', {
        params: { documento },
      })
      setProtocolos(data)
    } catch (err: any) {
      setErro(
        err?.response?.data?.mensagem ??
          'Não foi possível localizar um protocolo para o CPF/CNS informado.',
      )
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="min-h-screen">
      <header className="border-b bg-white">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center gap-2">
          <div className="h-8 w-8 rounded-lg bg-brand-teal" />
          <span className="font-bold text-lg text-brand-navy">Fila Saúde</span>
        </div>
      </header>

      <section className="max-w-3xl mx-auto px-6 py-14 text-center">
        <p className="text-brand-teal font-semibold text-sm tracking-wide uppercase mb-2">
          Acompanhamento de Protocolo
        </p>
        <h1 className="text-3xl md:text-4xl font-extrabold text-brand-navy mb-3">
          Consulte seu atendimento
        </h1>
        <p className="text-gray-500 mb-8">
          Informe seu CPF ou CNS (Cartão Nacional de Saúde) para acompanhar, em tempo real,
          a posição na fila e cada etapa do seu processo.
        </p>

        <form onSubmit={buscar} className="flex flex-col sm:flex-row gap-3 justify-center">
          <input
            value={documento}
            onChange={(e) => setDocumento(e.target.value)}
            placeholder="CPF ou CNS"
            className="flex-1 max-w-sm rounded-xl border border-gray-300 px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-brand-teal"
          />
          <button
            type="submit"
            disabled={carregando || documento.trim() === ''}
            className="rounded-xl bg-brand-navy text-white px-6 py-3 text-sm font-semibold hover:bg-brand-navy-dark disabled:opacity-50 transition-colors"
          >
            {carregando ? 'Buscando…' : 'Consultar'}
          </button>
        </form>

        {erro && (
          <p className="mt-6 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3">
            {erro}
          </p>
        )}
      </section>

      {protocolos && (
        <section className="max-w-3xl mx-auto px-6 pb-16 space-y-6">
          {protocolos.map((protocolo) => (
            <article
              key={protocolo.numeroProtocolo}
              className="rounded-2xl border bg-white shadow-sm p-6"
            >
              <div className="flex flex-wrap items-start justify-between gap-3 mb-4">
                <div>
                  <p className="text-xs text-gray-400">PROTOCOLO</p>
                  <p className="font-mono font-semibold text-brand-navy">
                    {protocolo.numeroProtocolo}
                  </p>
                </div>
                <StatusBadge status={protocolo.status} />
              </div>

              <p className="text-lg font-bold text-gray-900">{protocolo.nomePaciente}</p>
              <p className="text-sm text-gray-500 mb-4">{protocolo.nomeProcedimento}</p>

              <div className="grid grid-cols-2 gap-3 mb-6">
                <div className="rounded-xl bg-gray-50 px-4 py-3">
                  <p className="text-xs text-gray-400">Entrou na fila em</p>
                  <p className="font-semibold text-sm">
                    {new Date(protocolo.dataInclusao).toLocaleDateString('pt-BR')}
                  </p>
                </div>
                <div className="rounded-xl bg-brand-teal-light px-4 py-3">
                  <p className="text-xs text-gray-500">
                    {protocolo.posicaoFila ? 'Posição na fila' : 'Previsão'}
                  </p>
                  <p className="font-semibold text-sm text-brand-navy">
                    {protocolo.posicaoFila
                      ? `${protocolo.posicaoFila}º`
                      : protocolo.dataPrevista
                        ? new Date(protocolo.dataPrevista).toLocaleDateString('pt-BR')
                        : '—'}
                  </p>
                </div>
              </div>

              {protocolo.etapas.length > 0 && (
                <>
                  <p className="text-sm font-semibold text-gray-700 mb-4">Ciclo de atendimento</p>
                  <Timeline etapas={protocolo.etapas} />
                </>
              )}
            </article>
          ))}
        </section>
      )}
    </div>
  )
}
