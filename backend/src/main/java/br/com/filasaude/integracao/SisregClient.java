package br.com.filasaude.integracao;

/**
 * Contrato para a integração com o SISREG III (Sistema Nacional de
 * Regulação) -- uma das integrações obrigatórias confirmadas no
 * levantamento de requisitos.
 *
 * PENDENTE DE IMPLEMENTAÇÃO REAL: o SISREG III não tem API pública
 * documentada para terceiros; o acesso programático depende de acordo
 * técnico com o DATASUS/gestor estadual, caso a caso. Este contrato deixa
 * o ponto de extensão pronto para os dois sentidos de sincronização mais
 * prováveis (a definir com o cliente quando o acesso estiver disponível):
 * enviar uma solicitação de regulação criada aqui para o SISREG, e
 * consultar o status de uma solicitação já existente lá.
 *
 * Até a implementação real, qualquer chamada deve lançar
 * IntegracaoNaoConfiguradaException; o motor de priorização e a fila
 * internos (já implementados) continuam sendo a fonte de verdade.
 */
public interface SisregClient {

    /** Envia uma solicitação de regulação ao SISREG; retorna o identificador da solicitação no SISREG. */
    String enviarSolicitacao(SolicitacaoRegulacao solicitacao);

    /** Consulta o status atual de uma solicitação já enviada ao SISREG. */
    String consultarStatus(String idSolicitacaoSisreg);

    record SolicitacaoRegulacao(
            String numeroProtocoloLocal,
            String cnsPaciente,
            String codigoCnesUnidade,
            String procedimento
    ) {
    }
}
