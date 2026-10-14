package br.com.filasaude.controller;

import br.com.filasaude.dto.transparencia.TransparenciaConfigRequest;
import br.com.filasaude.dto.transparencia.TransparenciaConfigResponse;
import br.com.filasaude.dto.transparencia.TransparenciaResponse;
import br.com.filasaude.service.TransparenciaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Configuração da página de transparência ativa. Restrita a Admin (ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin/transparencia")
public class TransparenciaAdminController {

    private final TransparenciaService transparenciaService;

    public TransparenciaAdminController(TransparenciaService transparenciaService) {
        this.transparenciaService = transparenciaService;
    }

    @GetMapping("/config")
    public TransparenciaConfigResponse config() {
        return transparenciaService.obterConfig();
    }

    @PutMapping("/config")
    public TransparenciaConfigResponse salvar(@Valid @RequestBody TransparenciaConfigRequest request) {
        return transparenciaService.atualizarConfig(request);
    }

    /** O que seria publicado agora, mesmo com a página ainda desligada. */
    @GetMapping("/previa")
    public TransparenciaResponse previa() {
        return transparenciaService.gerarPrevia();
    }
}
