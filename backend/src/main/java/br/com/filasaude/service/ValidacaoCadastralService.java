package br.com.filasaude.service;

import br.com.filasaude.dto.validacao.ValidacaoDocumentoResponse;
import br.com.filasaude.integracao.CadsusClient;
import br.com.filasaude.integracao.IntegracaoNaoConfiguradaException;
import org.springframework.stereotype.Service;

/**
 * Validação de CPF/CNS do paciente (item do roadmap "validação de CNS/CPF
 * via CADSUS"): hoje o cadastro de paciente (ver PacienteService) só
 * confere o FORMATO do documento (quantidade de dígitos). Este serviço é o
 * ponto único que, além do formato, tenta confirmar o documento contra o
 * cadastro oficial do CADSUS -- e degrada sem erro para "só formato" quando
 * essa integração ainda não está disponível (ver CadsusClientIndisponivel),
 * que é o caso até a prefeitura ter acesso homologado ao webservice.
 *
 * Deliberadamente não chamado automaticamente por
 * PacienteService#cadastrar/atualizar: é uma ação explícita do operador
 * ("Validar no CADSUS"), não um bloqueio ao cadastro manual, que precisa
 * continuar funcionando mesmo sem a integração.
 */
@Service
public class ValidacaoCadastralService {

    private final CadsusClient cadsusClient;

    public ValidacaoCadastralService(CadsusClient cadsusClient) {
        this.cadsusClient = cadsusClient;
    }

    public ValidacaoDocumentoResponse validarCpf(String cpfBruto) {
        String cpf = somenteDigitos(cpfBruto);
        if (cpf.length() != 11) {
            return ValidacaoDocumentoResponse.formatoInvalido("CPF", cpf, "CPF inválido: deve conter 11 dígitos.");
        }
        try {
            return cadsusClient.consultarPorCpf(cpf)
                    .map(usuario -> ValidacaoDocumentoResponse.encontrado("CPF", cpf, usuario))
                    .orElseGet(() -> ValidacaoDocumentoResponse.naoEncontrado("CPF", cpf));
        } catch (IntegracaoNaoConfiguradaException e) {
            return ValidacaoDocumentoResponse.semIntegracao("CPF", cpf);
        }
    }

    public ValidacaoDocumentoResponse validarCns(String cnsBruto) {
        String cns = somenteDigitos(cnsBruto);
        if (cns.length() != 15) {
            return ValidacaoDocumentoResponse.formatoInvalido("CNS", cns, "CNS inválido: deve conter 15 dígitos.");
        }
        try {
            return cadsusClient.consultarPorCns(cns)
                    .map(usuario -> ValidacaoDocumentoResponse.encontrado("CNS", cns, usuario))
                    .orElseGet(() -> ValidacaoDocumentoResponse.naoEncontrado("CNS", cns));
        } catch (IntegracaoNaoConfiguradaException e) {
            return ValidacaoDocumentoResponse.semIntegracao("CNS", cns);
        }
    }

    private String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
