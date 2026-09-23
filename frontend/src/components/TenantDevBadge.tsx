import { resolverTenantId } from '../api/tenant'

const isLocal = (() => {
  const host = window.location.hostname
  return host === 'localhost' || host === '127.0.0.1' || /^\d+\.\d+\.\d+\.\d+$/.test(host)
})()

/**
 * Só aparece em ambiente local (sem subdomínio real de prefeitura), onde o
 * tenant é escolhido manualmente (ver api/tenant.ts) -- existe só para deixar
 * claro, durante testes, qual prefeitura está sendo visualizada no momento e
 * evitar confundir a identidade visual de uma prefeitura com a de outra.
 * Em produção (acesso por subdomínio real) este componente não renderiza nada.
 */
export function TenantDevBadge() {
  if (!isLocal) return null

  return (
    <div className="fixed bottom-3 right-3 z-50 rounded-full bg-gray-900/90 text-white text-[11px] px-3 py-1.5 shadow-lg backdrop-blur-sm">
      Tenant local: <span className="font-mono font-semibold">{resolverTenantId()}</span>
      <span className="text-white/50"> · troque com ?tenant=slug</span>
    </div>
  )
}