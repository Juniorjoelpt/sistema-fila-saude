import { useRef, useState } from 'react'
import { api } from '../api/client'
import type { ImportacaoResultado } from '../api/types'

/**
 * Upload de planilha CSV para importação em lote (item 3.4 do levantamento
 * de requisitos, Fase 2). Reutilizado nas telas de Procedimentos e
 * Pacientes — o endpoint e as colunas esperadas variam por uso.
 */
export function ImportadorCsv({
  endpoint,
  colunasEsperadas,
  onImportado,
}: {
  endpoint: string
  colunasEsperadas: string
  onImportado: () => void
}) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [enviando, setEnviando] = useState(false)
  const [resultado, setResultado] = useState<ImportacaoResultado | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  async function handleArquivoSelecionado(e: React.ChangeEvent<HTMLInputElement>) {
    const arquivo = e.target.files?.[0]
    if (!arquivo) return

    setEnviando(true)
    setErro(null)
    setResultado(null)
    try {
      const formData = new FormData()
      formData.append('arquivo', arquivo)
      const { data } = await api.post<ImportacaoResultado>(endpoint, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setResultado(data)
      onImportado()
    } catch {
      setErro('Não foi possível importar o arquivo.')
    } finally {
      setEnviando(false)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  return (
    <div className="rounded-2xl border border-dashed bg-gray-50 p-4">
      <p className="text-xs font-semibold text-gray-700 mb-1">Importação em lote (CSV)</p>
      <p className="text-xs text-gray-400 mb-3">Colunas esperadas: {colunasEsperadas}</p>
      <label className="inline-block rounded-lg bg-white border border-gray-300 px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-100 cursor-pointer">
        {enviando ? 'Importando…' : 'Escolher arquivo .csv'}
        <input
          ref={inputRef}
          type="file"
          accept=".csv,text/csv"
          onChange={handleArquivoSelecionado}
          disabled={enviando}
          className="hidden"
        />
      </label>

      {erro && <p className="text-xs text-red-600 mt-2">{erro}</p>}

      {resultado && (
        <div className="mt-3 text-xs">
          <p className="text-gray-700 font-semibold">
            {resultado.importados} de {resultado.totalLinhas} linha(s) importada(s) com sucesso.
          </p>
          {resultado.erros.length > 0 && (
            <ul className="mt-2 space-y-0.5 max-h-32 overflow-y-auto">
              {resultado.erros.map((e, idx) => (
                <li key={idx} className="text-red-600">
                  Linha {e.linha}: {e.motivo}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
