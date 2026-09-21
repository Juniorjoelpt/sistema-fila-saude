package br.com.filasaude.controller;

import br.com.filasaude.dto.publico.ProtocoloPublicoResponse;
import br.com.filasaude.service.ProtocoloPublicoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rota pública (sem autenticação) para o cidadão acompanhar seu protocolo —
 * item 3.1 do levantamento de requisitos. Exige apenas o header X-Tenant-Id
 * (resolvido pelo frontend a partir do domínio/subdomínio da prefeitura).
 */
@RestController
@RequestMapping("/api/public/protocolo")
public class ProtocoloPublicoController {

    private final ProtocoloPublicoService protocoloPublicoService;

    public ProtocoloPublicoController(ProtocoloPublicoService protocoloPublicoService) {
        this.protocoloPublicoService = protocoloPublicoService;
    }

    @GetMapping
    public List<ProtocoloPublicoResponse> consultar(@RequestParam("documento") String documento) {
        return protocoloPublicoService.consultarPorDocumento(documento);
    }
}
