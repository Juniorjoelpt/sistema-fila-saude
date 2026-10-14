package br.com.filasaude.controller;

import br.com.filasaude.dto.auditoria.ExportacaoArquivo;
import br.com.filasaude.dto.auditoria.LogAuditoriaResponse;
import br.com.filasaude.dto.common.PageResponse;
import br.com.filasaude.service.AuditoriaExportService;
import br.com.filasaude.service.AuditoriaService;
import br.com.filasaude.service.DossieProtocoloService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final AuditoriaExportService auditoriaExportService;
    private final DossieProtocoloService dossieProtocoloService;

    public AuditoriaController(AuditoriaService auditoriaService,
                               AuditoriaExportService auditoriaExportService,
                               DossieProtocoloService dossieProtocoloService) {
        this.auditoriaService = auditoriaService;
        this.auditoriaExportService = auditoriaExportService;
        this.dossieProtocoloService = dossieProtocoloService;
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

    /**
     * Roadmap #7: exporta a trilha filtrada para órgãos de controle (TCE/MP) em
     * pdf, csv ou json. O SHA-256 do arquivo vai no cabeçalho X-Hash-SHA256 e fica
     * gravado na própria trilha.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(defaultValue = "pdf") String formato,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String entidade,
            @RequestParam(required = false) Long entidadeId) {
        return arquivo(auditoriaExportService.exportar(formato, acao, usuarioId, dataInicio, dataFim, entidade, entidadeId));
    }

    /** Dossiê de um protocolo (status, prioridade, etapas e trilha), em pdf ou json. */
    @GetMapping("/protocolos/{id}/dossie")
    public ResponseEntity<byte[]> dossie(@PathVariable Long id, @RequestParam(defaultValue = "pdf") String formato) {
        return arquivo(dossieProtocoloService.gerar(id, formato));
    }

    private static ResponseEntity<byte[]> arquivo(ExportacaoArquivo a) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + a.nomeArquivo() + "\"")
                .header("X-Hash-SHA256", a.sha256())
                .header("X-Total-Registros", String.valueOf(a.registros()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(a.contentType()))
                .body(a.conteudo());
    }
}
