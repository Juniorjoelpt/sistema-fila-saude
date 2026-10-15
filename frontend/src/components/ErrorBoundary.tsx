import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props {
  children: ReactNode
}

interface State {
  temErro: boolean
}

/**
 * Bug crítico de revisão corrigido: não existia nenhum Error Boundary em
 * toda a aplicação. Qualquer exceção não tratada durante a renderização
 * (ex.: acesso a uma propriedade undefined vinda de uma resposta inesperada
 * da API) derrubava a árvore React inteira e o usuário ficava com uma tela
 * branca, sem nenhuma mensagem nem opção de recuperação. Este componente
 * captura esses erros e mostra uma tela de recuperação simples, em vez de
 * deixar o app inteiro quebrado silenciosamente.
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { temErro: false }

  static getDerivedStateFromError(): State {
    return { temErro: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // eslint-disable-next-line no-console
    console.error('Erro não tratado capturado pelo ErrorBoundary:', error, info.componentStack)
  }

  private recarregar = () => {
    window.location.href = '/'
  }

  render() {
    if (!this.state.temErro) {
      return this.props.children
    }

    return (
      <div className="min-h-screen flex items-center justify-center px-6">
        <div className="max-w-md w-full rounded-2xl border bg-white shadow-sm p-8 text-center">
          <p className="text-3xl mb-3">⚠️</p>
          <h1 className="text-lg font-bold text-gray-900 mb-2">Ocorreu um erro inesperado</h1>
          <p className="text-sm text-gray-500 mb-6">
            Algo deu errado ao carregar esta página. Você pode tentar recarregar; se o problema
            continuar, entre em contato com o suporte.
          </p>
          <button
            onClick={this.recarregar}
            className="btn-primary rounded-lg text-white px-5 py-2.5 text-sm font-semibold"
          >
            Recarregar
          </button>
        </div>
      </div>
    )
  }
}
