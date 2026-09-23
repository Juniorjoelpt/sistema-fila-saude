package br.com.filasaude.controller;

import br.com.filasaude.dto.auditoria.LogAuditoriaResponse;
import br.com.filasaude.dto.common.PageResponse;
import br.com.filasaude.service.AuditoriaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Consulta ao log de auditoria geral do sistema (item 3.5). Restrita a
 * Admin (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public PageResponse<LogAuditoriaResponse> listar(
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditoriaService.listar(acao, usuarioId, dataInicio, dataFim, page, size);
    }
}
