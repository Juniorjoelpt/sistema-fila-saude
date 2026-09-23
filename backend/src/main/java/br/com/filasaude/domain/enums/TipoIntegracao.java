package br.com.filasaude.domain.enums;

/**
 * Integrações obrigatórias com sistemas do Ministério da Saúde, confirmadas
 * no levantamento de requisitos (Decisões Confirmadas / item 3 e seção
 * "Já definido"): e-SUS (prontuário eletrônico), SISREG (regulação de
 * vagas) e CNES (Cadastro Nacional de Estabelecimentos de Saúde).
 */
public enum TipoIntegracao {
    ESUS,
    SISREG,
    CNES
}
