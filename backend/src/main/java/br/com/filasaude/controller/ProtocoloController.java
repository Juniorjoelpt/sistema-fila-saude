package br.com.filasaude.controller;

import br.com.filasaude.domain.enums.CategoriaPrioridade;
import br.com.filasaude.domain.enums.StatusEtapa;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.common.PageResponse;
import br.com.filasaude.dto.protocolo.DistribuirVagasRequest;
import br.com.filasaude.dto.protocolo.MudarStatusRequest;
import br.com.filasaude.dto.protocolo.ProtocoloCreateRequest;
import br.com.filasaude.dto.protocolo.ProtocoloDetalheResponse;
import br.com.filasaude.dto.protocolo.ProtocoloResponse;
import br.com.filasaude.service.ProtocoloService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestão de fila e regulação (item 3.2). Rotas autenticadas, restritas a
 * ACS/Regulador/Admin (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/fila")
public class ProtocoloController {

    private final ProtocoloService protocoloService;

    public ProtocoloController(ProtocoloService protocoloService) {
        this.protocoloService = protocoloService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProtocoloResponse criar(@Valid @RequestBody ProtocoloCreateRequest request) {
        return protocoloService.criar(request);
    }

    @GetMapping
    public PageResponse<ProtocoloResponse> listar(
            @RequestParam(required = false) CategoriaPrioridade categoria,
            @RequestParam(required = false) StatusProtocolo status,
            @RequestParam(required = false) Long procedimentoId,
            @RequestParam(required = false) Long acsResponsavelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return protocoloService.listarFila(categoria, status, procedimentoId, acsResponsavelId, page, size);
    }

    @GetMapping("/{id}")
    public ProtocoloDetalheResponse buscarDetalhe(@PathVariable Long id) {
        return protocoloService.buscarDetalhe(id);
    }

    @PatchMapping("/{id}/status")
    public ProtocoloResponse mudarStatus(@PathVariable Long id, @Valid @RequestBody MudarStatusRequest request) {
        return protocoloService.mudarStatus(id, request.novoStatus(), request.observacao());
    }

    @PatchMapping("/{protocoloId}/etapas/{etapaId}")
    public ProtocoloResponse marcarEtapa(@PathVariable Long protocoloId, @PathVariable Long etapaId,
                                          @RequestParam StatusEtapa status) {
        return protocoloService.marcarEtapa(protocoloId, etapaId, status);
    }

    @PostMapping("/distribuir-vagas")
    public List<ProtocoloResponse> distribuirVagas(@Valid @RequestBody DistribuirVagasRequest request) {
        return protocoloService.distribuirVagas(request);
    }
}
