package br.com.filasaude.controller;

import br.com.filasaude.domain.enums.TipoIntegracao;
import br.com.filasaude.dto.integracao.IntegracaoConfigRequest;
import br.com.filasaude.dto.integracao.IntegracaoConfigResponse;
import br.com.filasaude.service.IntegracaoConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Configuração das integrações obrigatórias com sistemas do Ministério da
 * Saúde (e-SUS, SISREG, CNES). Restrita a Admin (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/integracoes")
public class IntegracaoController {

    private final IntegracaoConfigService integracaoConfigService;

    public IntegracaoController(IntegracaoConfigService integracaoConfigService) {
        this.integracaoConfigService = integracaoConfigService;
    }

    @GetMapping
    public List<IntegracaoConfigResponse> listar() {
        return integracaoConfigService.listar();
    }

    @PutMapping("/{tipo}")
    public IntegracaoConfigResponse salvar(@PathVariable TipoIntegracao tipo, @Valid @RequestBody IntegracaoConfigRequest request) {
        return integracaoConfigService.salvar(tipo, request);
    }
}
