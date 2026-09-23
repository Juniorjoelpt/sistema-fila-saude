package br.com.filasaude.controller;

import br.com.filasaude.dto.importacao.ImportacaoResponse;
import br.com.filasaude.service.ImportacaoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Importação em lote de procedimentos e pacientes via planilha (item 3.4,
 * Fase 2). Restrito a Regulador/Admin (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/importacao")
public class ImportacaoController {

    private final ImportacaoService importacaoService;

    public ImportacaoController(ImportacaoService importacaoService) {
        this.importacaoService = importacaoService;
    }

    @PostMapping("/procedimentos")
    public ImportacaoResponse importarProcedimentos(@RequestParam("arquivo") MultipartFile arquivo) {
        return importacaoService.importarProcedimentos(arquivo);
    }

    @PostMapping("/pacientes")
    public ImportacaoResponse importarPacientes(@RequestParam("arquivo") MultipartFile arquivo) {
        return importacaoService.importarPacientes(arquivo);
    }
}
