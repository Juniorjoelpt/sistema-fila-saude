package br.com.filasaude.integracao;

import java.util.Optional;

/**
 * Contrato para a integração com o CNES (Cadastro Nacional de
 * Estabelecimentos de Saúde) -- uma das integrações obrigatórias
 * confirmadas no levantamento de requisitos.
 *
 * PENDENTE DE IMPLEMENTAÇÃO REAL: o Ministério da Saúde não publica uma API
 * pública e documentada do CNES com o mesmo nível de acesso do portal
 * público de consulta (cnes.datasus.gov.br); o acesso programático
 * geralmente depende de convênio/credencial específica com o DATASUS. Este
 * contrato existe para já deixar o ponto de extensão pronto: quando a
 * prefeitura (ou a MS Soluções) tiver acesso oficial, basta implementar
 * esta interface e registrá-la como @Service -- o restante do sistema
 * (cadastro de Unidades de Saúde, que já tem o campo codigoCnes) não
 * precisa mudar.
 *
 * Até lá, qualquer chamada deve lançar IntegracaoNaoConfiguradaException.
 */
public interface CnesClient {

    /**
     * Busca os dados oficiais de um estabelecimento de saúde pelo código
     * CNES, para pré-preencher o cadastro de Unidade de Saúde.
     */
    Optional<EstabelecimentoCnes> buscarPorCodigo(String codigoCnes);

    record EstabelecimentoCnes(
            String codigoCnes,
            String nome,
            String endereco,
            Double latitude,
            Double longitude
    ) {
    }
}
