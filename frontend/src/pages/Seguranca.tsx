import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import { Spinner } from '../components/Spinner'
import type { TwoFactorSetup, TwoFactorStatus } from '../api/types'

export function Seguranca() {
  const [status, setStatus] = useState<TwoFactorStatus | null>(null)
  const [carregando, setCarregando] = useState(true)

  const [setup, setSetup] = useState<TwoFactorSetup | null>(null)
  const [codigo, setCodigo] = useState('')
  const [senhaDesabilitar, setSenhaDesabilitar] = useState('')
  const [mostrarDesabilitar, setMostrarDesabilitar] = useState(false)

  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [sucesso, setSucesso] = useState<string | null>(null)

  function carregarStatus() {
    setCarregando(true)
    api
      .get<TwoFactorStatus>('/api/me/2fa')
      .then((res) => setStatus(res.data))
      .finally(() => setCarregando(false))
  }

  useEffect(() => {
    carregarStatus()
  }, [])

  async function iniciarConfiguracao() {
    setErro(null)
    setSucesso(null)
    setEnviando(true)
    try {
      const res = await api.post<TwoFactorSetup>('/api/me/2fa/iniciar')
      setSetup(res.data)
    } catch (err: any) {
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível iniciar a configuração do 2FA.')
    } finally {
      setEnviando(false)
    }
  }

  async function confirmarConfiguracao() {
    setErro(null)
    setEnviando(true)
    try {
      await api.post('/api/me/2fa/confirmar', { codigo })
      setSetup(null)
      setCodigo('')
      setSucesso('Autenticação em dois fatores ativada com sucesso.')
      carregarStatus()
    } catch (err: any) {
      setErro(err?.response?.data?.mensagem ?? 'Código inválido.')
    } finally {
      setEnviando(false)
    }
  }

  async function desabilitar() {
    setErro(null)
    setEnviando(true)
    try {
      await api.post('/api/me/2fa/desabilitar', { senha: senhaDesabilitar })
      setSenhaDesabilitar('')
      setMostrarDesabilitar(false)
      setSucesso('Autenticação em dois fatores desativada.')
      carregarStatus()
    } catch (err: any) {
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível desativar o 2FA.')
    } finally {
      setEnviando(false)
    }
  }

  const secretFormatado = setup?.secretBase32.match(/.{1,4}/g)?.join(' ') ?? ''

  return (
    <AdminLayout>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Segurança</h1>

      <div className="max-w-xl rounded-2xl border bg-white shadow-sm p-6">
        <h2 className="text-sm font-semibold text-gray-700 mb-1">Autenticação em dois fatores (2FA)</h2>
        <p className="text-sm text-gray-500 mb-4">
          Além da senha, exige um código de 6 dígitos gerado por um app autenticador (Google Authenticator, Authy,
          Microsoft Authenticator etc.) a cada login. Opcional, mas recomendado.
        </p>

        {carregando && (
          <div className="flex items-center gap-2 text-gray-400 text-sm">
            <Spinner /> Carregando…
          </div>
        )}

        {!carregando && sucesso && <p className="text-sm text-brand-teal font-medium mb-4">{sucesso}</p>}

        {!carregando && status?.habilitado && !mostrarDesabilitar && (
          <div className="flex items-center justify-between rounded-lg bg-teal-50 border border-brand-teal px-4 py-3">
            <span className="text-sm font-semibold text-brand-navy">✓ 2FA ativado nesta conta</span>
            <button
              onClick={() => setMostrarDesabilitar(true)}
              className="text-sm text-red-600 hover:text-red-700 font-medium"
            >
              Desativar
            </button>
          </div>
        )}

        {!carregando && status?.habilitado && mostrarDesabilitar && (
          <div className="space-y-3">
            <p className="text-sm text-gray-600">Confirme sua senha para desativar o 2FA.</p>
            <input
              type="password"
              value={senhaDesabilitar}
              onChange={(e) => setSenhaDesabilitar(e.target.value)}
              placeholder="Sua senha"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
            {erro && <p className="text-sm text-red-600">{erro}</p>}
            <div className="flex gap-2">
              <button
                onClick={desabilitar}
                disabled={enviando || !senhaDesabilitar}
                className="rounded-lg bg-red-600 text-white px-4 py-2 text-sm font-semibold disabled:opacity-40"
              >
                {enviando ? 'Desativando…' : 'Confirmar desativação'}
              </button>
              <button
                onClick={() => {
                  setMostrarDesabilitar(false)
                  setSenhaDesabilitar('')
                  setErro(null)
                }}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-600"
              >
                Cancelar
              </button>
            </div>
          </div>
        )}

        {!carregando && !status?.habilitado && !setup && (
          <button
            onClick={iniciarConfiguracao}
            disabled={enviando}
            className="rounded-lg bg-brand-navy text-white px-4 py-2 text-sm font-semibold disabled:opacity-40"
          >
            {enviando ? 'Gerando…' : 'Ativar 2FA'}
          </button>
        )}

        {!carregando && setup && (
          <div className="space-y-4">
            <ol className="text-sm text-gray-600 space-y-3 list-decimal list-inside">
              <li>Abra um app autenticador no seu celular.</li>
              <li>Escaneie o QR Code abaixo (ou digite o código manualmente).</li>
              <li>Digite o código de 6 dígitos que o app mostrar para confirmar.</li>
            </ol>

            <img
              src={`data:image/png;base64,${setup.qrCodeBase64Png}`}
              alt="QR Code para configurar o 2FA"
              className="rounded-lg border w-56 h-56"
            />

            <div>
              <p className="text-xs text-gray-500 mb-1">Não consegue escanear? Digite este código manualmente:</p>
              <code className="block rounded-lg bg-gray-50 border px-3 py-2 text-sm font-mono tracking-wider">
                {secretFormatado}
              </code>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Código de confirmação</label>
              <input
                type="text"
                inputMode="numeric"
                maxLength={6}
                value={codigo}
                onChange={(e) => setCodigo(e.target.value.replace(/\D/g, ''))}
                placeholder="000000"
                className="w-40 rounded-lg border border-gray-300 px-3 py-2 text-sm tracking-[0.3em] text-center font-mono"
              />
            </div>

            {erro && <p className="text-sm text-red-600">{erro}</p>}

            <div className="flex gap-2">
              <button
                onClick={confirmarConfiguracao}
                disabled={enviando || codigo.length !== 6}
                className="rounded-lg bg-brand-teal text-white px-4 py-2 text-sm font-semibold disabled:opacity-40"
              >
                {enviando ? 'Confirmando…' : 'Confirmar e ativar'}
              </button>
              <button
                onClick={() => {
                  setSetup(null)
                  setCodigo('')
                  setErro(null)
                }}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-600"
              >
                Cancelar
              </button>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  )
}
