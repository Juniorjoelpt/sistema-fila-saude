import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, resolverUrlAsset } from '../api/client'
import type { TenantBranding } from '../api/types'

const TenantBrandingContext = createContext<TenantBranding | null>(null)

const PADRAO: TenantBranding = { nomeMunicipio: null, corPrimaria: null, corSecundaria: null, logoUrl: null }

/**
 * Em várias telas, `--color-brand-navy` / `--color-brand-teal` são usadas como
 * fundo de botões, badges e avatares que têm texto branco fixo por cima (ex.:
 * "bg-brand-teal text-white"). Se uma prefeitura escolher uma cor secundária/
 * primária clara (ex.: branco, amarelo-claro), esse texto branco fica ilegível.
 *
 * Em vez de caçar e ajustar dinamicamente a cor do texto em cada botão/badge
 * do sistema (frágil e espalhado por muitas telas), a cor da prefeitura é
 * escurecida automaticamente aqui -- na origem -- sempre que estiver clara
 * demais para manter contraste seguro com texto branco. O matiz (hue) e a
 * saturação escolhidos pela prefeitura são preservados; só o brilho (L do
 * HSL) é reduzido quando necessário.
 */
const LUMINOSIDADE_MAXIMA = 45 // % -- acima disso, texto branco por cima perde legibilidade

function hexParaHsl(hex: string): { h: number; s: number; l: number } | null {
  const m = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex.trim())
  if (!m) return null
  const r = parseInt(m[1], 16) / 255
  const g = parseInt(m[2], 16) / 255
  const b = parseInt(m[3], 16) / 255
  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  let h = 0
  let s = 0
  const l = (max + min) / 2
  const d = max - min
  if (d !== 0) {
    s = d / (1 - Math.abs(2 * l - 1))
    switch (max) {
      case r:
        h = ((g - b) / d) % 6
        break
      case g:
        h = (b - r) / d + 2
        break
      default:
        h = (r - g) / d + 4
    }
    h *= 60
    if (h < 0) h += 360
  }
  return { h, s: s * 100, l: l * 100 }
}

function hslParaHex(h: number, s: number, l: number): string {
  s /= 100
  l /= 100
  const k = (n: number) => (n + h / 30) % 12
  const a = s * Math.min(l, 1 - l)
  const f = (n: number) => l - a * Math.max(-1, Math.min(k(n) - 3, Math.min(9 - k(n), 1)))
  const paraHex = (n: number) =>
    Math.round(255 * f(n))
      .toString(16)
      .padStart(2, '0')
  return `#${paraHex(0)}${paraHex(8)}${paraHex(4)}`
}

/**
 * Garante contraste seguro para texto branco: se a cor informada pela
 * prefeitura for clara demais, devolve uma versão escurecida (mesmo matiz);
 * caso contrário, devolve a cor original sem alteração.
 */
function corSeguraParaTextoBranco(hex: string): string {
  const hsl = hexParaHsl(hex)
  if (!hsl) return hex // formato inesperado -- aplica como veio, sem travar a tela
  if (hsl.l <= LUMINOSIDADE_MAXIMA) return hex
  return hslParaHex(hsl.h, hsl.s, LUMINOSIDADE_MAXIMA)
}

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
          document.documentElement.style.setProperty('--color-brand-navy', corSeguraParaTextoBranco(data.corPrimaria))
        }
        if (data.corSecundaria) {
          document.documentElement.style.setProperty('--color-brand-teal', corSeguraParaTextoBranco(data.corSecundaria))
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
