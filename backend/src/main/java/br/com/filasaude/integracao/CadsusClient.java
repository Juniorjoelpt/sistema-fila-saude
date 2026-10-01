package br.com.filasaude.integracao;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Contrato para a integração com o CADSUS (Cartão Nacional de Saúde /
 * Cadastro de Usuários do SUS) -- item do roadmap "validação de CNS/CPF via
 * CADSUS". Hoje o cadastro de paciente só valida o FORMATO do documento
 * (quantidade de dígitos, ver PacienteService); consultar o CADSUS evita
 * que um CPF com formato válido, mas de OUTRA pessoa, vire um cadastro
 * errado -- e permite pré-preencher nome/data de nascimento a partir do
 * cadastro oficial.
 *
 * PENDENTE DE IMPLEMENTAÇÃO REAL: o webservice do CADSUS é exposto pelo
 * DATASUS a sistemas homologados (como o e-SUS/SISREG, ver EsusClient/
 * SisregClient), via certificado digital por município -- não é uma API
 * pública de uso imediato. Este contrato, a configuração em "Integrações"
 * (tipo CADSUS) e o endpoint de validação (ValidacaoCadastralController)
 * já deixam a integração pronta para ligar; {@link CadsusClientIndisponivel}
 * é a única implementação até a homologação, e sempre lança
 * IntegracaoNaoConfiguradaException. Até lá, a validação de documento
 * cadastral do paciente continua sendo só por formato.
 */
public interface CadsusClient {

    /** Consulta o cadastro oficial de um usuário do SUS pelo CPF. */
    Optional<UsuarioCadsus> consultarPorCpf(String cpf);

    /** Consulta o cadastro oficial de um usuário do SUS pelo CNS (Cartão Nacional de Saúde). */
    Optional<UsuarioCadsus> consultarPorCns(String cns);

    record UsuarioCadsus(
            String cpf,
            String cns,
            String nome,
            String nomeMae,
            LocalDate dataNascimento,
            boolean situacaoAtiva
    ) {
    }
}
