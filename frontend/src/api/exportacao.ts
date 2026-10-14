import { api } from './client'

export interface ResultadoExportacao {
  nomeArquivo: string
  sha256: string | null
  total: number | null
}

/** Lê a mensagem de erro de uma resposta de download (o corpo vem como Blob mesmo nos erros). */
async function mensagemDeErro(err: any, padrao: string): Promise<string> {
  try {
    const dados = err?.response?.data
    if (dados instanceof Blob) {
      const json = JSON.parse(await dados.text())
      return json?.mensagem ?? padrao
    }
    return dados?.mensagem ?? padrao
  } catch {
    return padrao
  }
}

/**
 * Baixa um arquivo gerado pelo backend (relatórios de auditoria) e devolve o SHA-256 e o total
 * informados nos cabeçalhos X-Hash-SHA256 / X-Total-Registros.
 */
export async function baixarExportacao(
  url: string,
  params: Record<string, unknown>,
  nomePadrao: string,
  mensagemPadrao: string,
): Promise<ResultadoExportacao> {
  try {
    const res = await api.get(url, { params, responseType: 'blob' })
    const disposicao: string = res.headers['content-disposition'] ?? ''
    const nomeArquivo = /filename="?([^";]+)"?/i.exec(disposicao)?.[1] ?? nomePadrao
    const link = document.createElement('a')
    const objeto = window.URL.createObjectURL(res.data as Blob)
    link.href = objeto
    link.setAttribute('download', nomeArquivo)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(objeto)
    const total = res.headers['x-total-registros']
    return {
      nomeArquivo,
      sha256: res.headers['x-hash-sha256'] ?? null,
      total: total != null ? Number(total) : null,
    }
  } catch (err: any) {
    throw new Error(await mensagemDeErro(err, mensagemPadrao))
  }
}
