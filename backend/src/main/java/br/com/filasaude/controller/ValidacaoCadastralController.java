package br.com.filasaude.controller;

import br.com.filasaude.dto.validacao.ValidacaoDocumentoResponse;
import br.com.filasaude.service.ValidacaoCadastralService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Validação de CPF/CNS do paciente contra o CADSUS (item do roadmap).
 * Mesmo nível de acesso de Pacientes -- ACS também cadastra paciente (ver
 * SecurityConfig), então também precisa poder validar o documento.
 */
@RestController
@RequestMapping("/api/validacao-cadastral")
public class ValidacaoCadastralController {

    private final ValidacaoCadastralService validacaoCadastralService;

    public ValidacaoCadastralController(ValidacaoCadastralService validacaoCadastralService) {
        this.validacaoCadastralService = validacaoCadastralService;
    }

    @GetMapping("/cpf")
    public ValidacaoDocumentoResponse validarCpf(@RequestParam String valor) {
        return validacaoCadastralService.validarCpf(valor);
    }

    @GetMapping("/cns")
    public ValidacaoDocumentoResponse validarCns(@RequestParam String valor) {
        return validacaoCadastralService.validarCns(valor);
    }
}
