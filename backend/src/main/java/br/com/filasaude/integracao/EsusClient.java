package br.com.filasaude.integracao;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Contrato para a integração com o e-SUS (prontuário eletrônico de atenção
 * primária) -- uma das integrações obrigatórias confirmadas no
 * levantamento de requisitos.
 *
 * PENDENTE DE IMPLEMENTAÇÃO REAL: o e-SUS Atenção Primária (e-SUS APS)
 * expõe integração via Thrift/REST para sistemas homologados pelo DATASUS,
 * o que exige processo de homologação específico por fornecedor -- não é
 * uma API pública de uso imediato. Este contrato deixa o ponto de extensão
 * pronto: buscar dados do paciente pelo CNS para pré-preencher o cadastro,
 * evitando redigitação de dados já existentes na Atenção Primária.
 *
 * Até a homologação/implementação real, qualquer chamada deve lançar
 * IntegracaoNaoConfiguradaException; o cadastro manual de pacientes (já
 * implementado) continua sendo o caminho padrão.
 */
public interface EsusClient {

    Optional<PacienteEsus> buscarPorCns(String cns);

    record PacienteEsus(
            String cns,
            String nome,
            LocalDate dataNascimento,
            String telefone
    ) {
    }
}
