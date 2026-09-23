package br.com.filasaude.integracao;

import br.com.filasaude.domain.enums.TipoIntegracao;

/**
 * Lançada quando uma funcionalidade tenta usar uma integração (e-SUS,
 * SISREG ou CNES) que ainda não foi configurada/ativada em
 * IntegracaoConfig para o tenant atual. Nenhuma dessas integrações é
 * obrigatória para o uso do sistema -- o cadastro manual continua
 * funcionando normalmente enquanto elas não estiverem disponíveis.
 */
public class IntegracaoNaoConfiguradaException extends RuntimeException {
    public IntegracaoNaoConfiguradaException(TipoIntegracao tipo) {
        super("Integração " + tipo + " não está configurada ou não está ativa para esta prefeitura");
    }
}
