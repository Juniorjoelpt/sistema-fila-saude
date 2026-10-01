package br.com.filasaude.controller;

import br.com.filasaude.dto.slaconfiguracao.SlaConfiguracaoRequest;
import br.com.filasaude.dto.slaconfiguracao.SlaConfiguracaoResponse;
import br.com.filasaude.dto.slaconfiguracao.SlaConfiguracoesResponse;
import br.com.filasaude.service.SlaConfiguracaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * SLA configurável por especialidade/procedimento (item do roadmap "Ganhos
 * rápidos"). Mesmo nível de acesso de Cotas/Procedimentos -- Regulador/Admin
 * (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/sla-configuracoes")
public class SlaConfiguracaoController {

    private final SlaConfiguracaoService slaConfiguracaoService;

    public SlaConfiguracaoController(SlaConfiguracaoService slaConfiguracaoService) {
        this.slaConfiguracaoService = slaConfiguracaoService;
    }

    @GetMapping
    public SlaConfiguracoesResponse listar() {
        return slaConfiguracaoService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SlaConfiguracaoResponse criar(@Valid @RequestBody SlaConfiguracaoRequest request) {
        return slaConfiguracaoService.criar(request);
    }

    @PutMapping("/{id}")
    public SlaConfiguracaoResponse atualizar(@PathVariable Long id, @Valid @RequestBody SlaConfiguracaoRequest request) {
        return slaConfiguracaoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        slaConfiguracaoService.excluir(id);
    }
}
