package br.com.filasaude.controller;

import br.com.filasaude.dto.protocolo.ClassificacaoSugeridaResponse;
import br.com.filasaude.service.ClassificacaoRiscoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Classificação de risco assistida (item do roadmap): sugere a categoria de
 * prioridade (Especial/Legal/Normal) a partir de dados objetivos do paciente
 * -- consultado pelo frontend ao montar o protocolo, tanto para um paciente
 * já cadastrado quanto para um sendo digitado agora em "Novo Protocolo"
 * (por isso recebe os dados soltos por parâmetro, em vez de um pacienteId:
 * um paciente novo ainda não tem ID no momento em que a sugestão é útil).
 */
@RestController
@RequestMapping("/api/classificacao-risco")
public class ClassificacaoRiscoController {

    private final ClassificacaoRiscoService classificacaoRiscoService;

    public ClassificacaoRiscoController(ClassificacaoRiscoService classificacaoRiscoService) {
        this.classificacaoRiscoService = classificacaoRiscoService;
    }

    @GetMapping("/sugestao")
    public ClassificacaoSugeridaResponse sugerir(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascimento,
            @RequestParam(defaultValue = "false") boolean pcd,
            @RequestParam(defaultValue = "false") boolean gestante) {
        return ClassificacaoSugeridaResponse.de(classificacaoRiscoService.sugerir(dataNascimento, pcd, gestante));
    }
}
