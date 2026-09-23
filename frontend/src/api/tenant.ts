const CHAVE_OVERRIDE = 'filasaude:tenant-override'

/**
 * Resolve o tenant (prefeitura) corrente no navegador. Em produção, cada
 * prefeitura acessa por um subdomínio próprio (ex.: portopi.filasaude.com.br),
 * do qual o slug é extraído automaticamente.
 *
 * Em desenvolvimento local (localhost/IP, sem subdomínio real), não há como
 * distinguir prefeituras pela URL -- por isso o app aceita alternar o tenant
 * local via query string, ex.: http://localhost:5173/?tenant=porto-pi. A
 * escolha fica salva no navegador (localStorage) e vale para as próximas
 * visitas, até ser trocada de novo ou removida (limpando o localStorage).
 * Sem nenhuma escolha feita, cai no padrão de VITE_TENANT_ID (ver .env.example).
 */
export function resolverTenantId(): string {
  const host = window.location.hostname
  const isLocal = host === 'localhost' || host === '127.0.0.1' || /^\d+\.\d+\.\d+\.\d+$/.test(host)

  if (isLocal) {
    try {
      const doQuery = new URLSearchParams(window.location.search).get('tenant')
      if (doQuery && doQuery.trim() !== '') {
        localStorage.setItem(CHAVE_OVERRIDE, doQuery.trim().toLowerCase())
      }
      const salvo = localStorage.getItem(CHAVE_OVERRIDE)
      if (salvo) return salvo
    } catch {
      // localStorage indisponível (ex.: navegação privada) -- segue com o padrão abaixo.
    }
    return import.meta.env.VITE_TENANT_ID ?? 'demo'
  }

  const partes = host.split('.')
  return partes.length > 2 ? partes[0] : (import.meta.env.VITE_TENANT_ID ?? 'demo')
}