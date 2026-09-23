package br.com.filasaude.controller;

import br.com.filasaude.dto.cota.CotaAjusteRequest;
import br.com.filasaude.dto.cota.CotaAjusteResponse;
import br.com.filasaude.dto.cota.CotaCreateRequest;
import br.com.filasaude.dto.cota.CotaResponse;
import br.com.filasaude.service.CotaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

/**
 * Gestão de cotas por unidade de saúde e especialidade (item 3.3). Restrito
 * a Regulador/Admin — mesmo nível de acesso de Procedimentos/Unidades (ver
 * SecurityConfig).
 */
@RestController
@RequestMapping("/api/cotas")
public class CotaController {

    private final CotaService cotaService;

    public CotaController(CotaService cotaService) {
        this.cotaService = cotaService;
    }

    @GetMapping
    public List<CotaResponse> listar(
            @RequestParam(required = false) Long unidadeSaudeId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mesReferencia) {
        return cotaService.listar(unidadeSaudeId, mesReferencia);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CotaResponse criar(@Valid @RequestBody CotaCreateRequest request) {
        return cotaService.criar(request);
    }

    @PatchMapping("/{id}")
    public CotaResponse ajustar(@PathVariable Long id, @Valid @RequestBody CotaAjusteRequest request) {
        return cotaService.ajustar(id, request);
    }

    @GetMapping("/{id}/ajustes")
    public List<CotaAjusteResponse> historico(@PathVariable Long id) {
        return cotaService.historico(id);
    }
}
