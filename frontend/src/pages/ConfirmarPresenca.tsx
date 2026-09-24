import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import { useTenantBranding } from '../context/TenantBrandingContext'
import type { ConfirmacaoPresenca } from '../api/types'

/**
 * Tela pública de confirmação de presença (melhoria pós-MVP sobre o
 * agendamento de horário real): acessada pelo link enviado no e-mail de
 * lembrete um dia antes do atendimento (ver LembreteAgendamentoService).
 * Identidade provada apenas pela posse do token na URL -- sem login, sem
 * CPF/CNS -- para que o clique no e-mail resolva tudo em um passo.
 */
export function ConfirmarPresenca() {
  const branding = useTenantBranding()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''

  const [carregando, setCarregando] = useState(true)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [dados, setDados] = useState<ConfirmacaoPresenca | null>(null)

  useEffect(() => {
    if (!token) {
      setErro('Link inválido: token de confirmação não informado.')
      setCarregando(false)
      return
    }

    api
      .get<ConfirmacaoPresenca>(`/api/public/confirmacao/${token}`)
      .then((res) => setDados(res.data))
      .catch((err) => {
        setErro(
          err?.response?.data?.mensagem ??
            'Não foi possível localizar este agendamento. O link pode ter expirado.',
        )
      })
      .finally(() => setCarregando(false))
  }, [token])

  async function responder(acao: 'confirmar' | 'cancelar') {
    setEnviando(true)
    setErro(null)
    try {
      const { data } = await api.post<ConfirmacaoPresenca>(`/api/public/confirmacao/${token}/${acao}`)
      setDados(data)
    } catch (err: any) {
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível registrar sua resposta. Tente novamente.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="min-h-screen flex flex-col animate-fade-in">
      <header className="border-b bg-white/80 backdrop-blur-sm sticky top-0 z-10">
        <div className="max-w-3xl mx-auto px-6 py-4 flex items-center gap-2">
          {branding.logoUrl ? (
            <img
              src={branding.logoUrl}
              alt={branding.nomeMunicipio ?? 'Logo da Secretaria'}
              className="h-8 max-w-[160px] object-contain"
            />
          ) : (
            <div className="h-8 w-8 rounded-lg bg-brand-teal" />
          )}
          <span className="font-bold text-lg text-brand-navy">
            Fila Saúde{branding.nomeMunicipio ? ` · ${branding.nomeMunicipio}` : ''}
          </span>
        </div>
      </header>

      <section className="max-w-xl mx-auto px-6 py-14 w-full">
        <h1 className="text-2xl md:text-3xl font-extrabold text-brand-navy mb-2 text-center">
          Confirmação de presença
        </h1>

        {carregando && <p className="text-center text-gray-500 mt-8">Carregando…</p>}

        {!carregando && erro && !dados && (
          <p className="mt-6 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3 text-center">
            {erro}
          </p>
        )}

        {!carregando && dados && (
          <div className="rounded-2xl border bg-white shadow-sm p-6 mt-6">
            <p className="text-xs text-gray-400 mb-1">
              PROTOCOLO <span className="font-mono font-semibold text-brand-navy">{dados.numeroProtocolo}</span>
            </p>
            <p className="text-lg font-bold text-gray-900">{dados.nomePaciente}</p>
            <p className="text-sm text-gray-500 mb-4">{dados.nomeProcedimento}</p>

            <div className="rounded-xl bg-teal-50 border border-brand-teal px-4 py-3 mb-6">
              <p className="text-xs text-gray-500">Atendimento marcado para</p>
              <p className="font-semibold text-sm text-brand-navy">
                {dados.dataPrevista
                  ? new Date(dados.dataPrevista + 'T00:00:00').toLocaleDateString('pt-BR')
                  : '—'}
                {dados.horaAgendada ? ` às ${dados.horaAgendada.slice(0, 5)}` : ''}
              </p>
              {dados.nomeUnidadeSaude && (
                <p className="text-sm text-gray-700 mt-1">{dados.nomeUnidadeSaude}</p>
              )}
            </div>

            {dados.presencaConfirmacao === 'CONFIRMADA' && (
              <p className="rounded-xl bg-green-50 border border-green-200 text-green-700 text-sm font-semibold px-4 py-3 text-center">
                ✓ Presença confirmada. Te esperamos!
              </p>
            )}
            {dados.presencaConfirmacao === 'CANCELADA' && (
              <p className="rounded-xl bg-red-50 border border-red-100 text-red-600 text-sm font-semibold px-4 py-3 text-center">
                Presença cancelada. Se precisar, entre em contato com a Secretaria de Saúde para remarcar.
              </p>
            )}

            {dados.presencaConfirmacao === 'PENDENTE' && (
              <>
                {erro && (
                  <p className="mb-4 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3">
                    {erro}
                  </p>
                )}
                <div className="flex flex-col sm:flex-row gap-3">
                  <button
                    onClick={() => responder('confirmar')}
                    disabled={enviando}
                    className="flex-1 rounded-xl bg-brand-navy text-white py-3 text-sm font-semibold hover:bg-brand-navy-dark disabled:opacity-50"
                  >
                    {enviando ? 'Enviando…' : '✓ Confirmar presença'}
                  </button>
                  <button
                    onClick={() => responder('cancelar')}
                    disabled={enviando}
                    className="flex-1 rounded-xl border border-red-300 text-red-600 py-3 text-sm font-semibold hover:bg-red-50 disabled:opacity-50"
                  >
                    Não poderei comparecer
                  </button>
                </div>
              </>
            )}
          </div>
        )}
      </section>
    </div>
  )
}
