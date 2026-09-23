import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, resolverUrlAsset } from '../api/client'
import type { TenantBranding } from '../api/types'

const TenantBrandingContext = createContext<TenantBranding | null>(null)

const PADRAO: TenantBranding = { nomeMunicipio: null, corPrimaria: null, corSecundaria: null, logoUrl: null }

/**
 * Busca a identidade visual do tenant (logo e cores da Secretaria -- item 3.6
 * do levantamento de requisitos) assim que o app carrega, antes de qualquer
 * login, e a aplica sobrescrevendo as variáveis CSS de tema (--color-brand-navy
 * / --color-brand-teal, definidas em index.css) usadas por todas as classes
 * utilitárias `bg-brand-*` / `text-brand-*` do Tailwind em todo o app. Também
 * disponibiliza logo/nome do município via contexto para os cabeçalhos das
 * telas pública e administrativa.
 */
export function TenantBrandingProvider({ children }: { children: ReactNode }) {
  const [branding, setBranding] = useState<TenantBranding>(PADRAO)

  useEffect(() => {
    api
      .get<TenantBranding>('/api/public/tenant/branding')
      .then(({ data }) => {
        // O logo pode vir como caminho relativo (upload direto, servido pela própria API)
        // ou URL externa absoluta -- resolvido aqui uma única vez para todo o app.
        setBranding({ ...data, logoUrl: resolverUrlAsset(data.logoUrl) })
        if (data.corPrimaria) {
          document.documentElement.style.setProperty('--color-brand-navy', data.corPrimaria)
        }
        if (data.corSecundaria) {
          document.documentElement.style.setProperty('--color-brand-teal', data.corSecundaria)
        }
      })
      .catch(() => {
        // Tenant sem identidade visual customizada (ou falha de rede) -- o app
        // segue normalmente com a paleta padrão do produto.
      })
  }, [])

  return <TenantBrandingContext.Provider value={branding}>{children}</TenantBrandingContext.Provider>
}

export function useTenantBranding(): TenantBranding {
  return useContext(TenantBrandingContext) ?? PADRAO
}
