/**
 * Resolve o tenant (prefeitura) corrente no navegador. Em produção, cada
 * prefeitura acessa por um subdomínio próprio (ex.: portopi.filasaude.com.br),
 * do qual o slug é extraído automaticamente. Em desenvolvimento local, usa a
 * variável de ambiente VITE_TENANT_ID (ver .env.example).
 */
export function resolverTenantId(): string {
  const host = window.location.hostname

  const isLocal = host === 'localhost' || host === '127.0.0.1' || /^\d+\.\d+\.\d+\.\d+$/.test(host)
  if (isLocal) {
    return import.meta.env.VITE_TENANT_ID ?? 'demo'
  }

  const partes = host.split('.')
  return partes.length > 2 ? partes[0] : (import.meta.env.VITE_TENANT_ID ?? 'demo')
}
