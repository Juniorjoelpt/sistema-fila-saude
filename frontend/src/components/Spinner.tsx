/** Indicador de carregamento simples e consistente, usado em toda a aplicação. */
export function Spinner({ className = '' }: { className?: string }) {
  return (
    <span
      className={`inline-block h-4 w-4 rounded-full border-2 border-gray-200 border-t-brand-teal animate-spin ${className}`}
    />
  )
}
