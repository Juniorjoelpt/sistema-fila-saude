package br.com.filasaude.dto.validacao;

import br.com.filasaude.integracao.CadsusClient;

import java.time.LocalDate;

/**
 * Resultado da validação de um CPF/CNS (item do roadmap "validação de
 * CNS/CPF via CADSUS"). Sempre traz o resultado da validação de FORMATO
 * (hoje disponível); "consultaCadsusDisponivel" indica se, além disso, foi
 * possível confirmar o documento contra o cadastro oficial do DATASUS --
 * falso até a integração real existir (ver CadsusClientIndisponivel), caso
 * em que "mensagem" explica isso ao operador sem travar o cadastro manual.
 */
public record ValidacaoDocumentoResponse(
        String tipoDocumento,
        String documento,
        boolean formatoValido,
        boolean consultaCadsusDisponivel,
        boolean encontradoNoCadsus,
        String nomeCadsus,
        LocalDate dataNascimentoCadsus,
        String mensagem
) {
    public static ValidacaoDocumentoResponse formatoInvalido(String tipoDocumento, String documento, String mensagem) {
        return new ValidacaoDocumentoResponse(tipoDocumento, documento, false, false, false, null, null, mensagem);
    }

    public static ValidacaoDocumentoResponse semIntegracao(String tipoDocumento, String documento) {
        return new ValidacaoDocumentoResponse(tipoDocumento, documento, true, false, false, null, null,
                "Formato válido. Validação automática contra o CADSUS não está disponível ainda nesta prefeitura "
                        + "-- confirme os dados manualmente com o paciente.");
    }

    public static ValidacaoDocumentoResponse encontrado(String tipoDocumento, String documento, CadsusClient.UsuarioCadsus usuario) {
        return new ValidacaoDocumentoResponse(tipoDocumento, documento, true, true, true,
                usuario.nome(), usuario.dataNascimento(),
                "Documento confirmado no CADSUS" + (usuario.situacaoAtiva() ? "." : " (cadastro inativo no CADSUS)."));
    }

    public static ValidacaoDocumentoResponse naoEncontrado(String tipoDocumento, String documento) {
        return new ValidacaoDocumentoResponse(tipoDocumento, documento, true, true, false, null, null,
                "Formato válido, mas este documento não foi encontrado no CADSUS -- confira se foi digitado corretamente.");
    }
}
