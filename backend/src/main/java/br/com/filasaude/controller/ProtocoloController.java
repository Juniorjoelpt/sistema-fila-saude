package br.com.filasaude.controller;

import br.com.filasaude.domain.enums.CategoriaPrioridade;
import br.com.filasaude.domain.enums.StatusEtapa;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.common.PageResponse;
import br.com.filasaude.dto.protocolo.AlterarPrioridadeRequest;
import br.com.filasaude.dto.protocolo.DistribuirVagasRequest;
import br.com.filasaude.dto.protocolo.MudarStatusRequest;
import br.com.filasaude.dto.protocolo.ProtocoloCreateRequest;
import br.com.filasaude.dto.protocolo.ProtocoloDetalheResponse;
import br.com.filasaude.dto.protocolo.ProtocoloResponse;
import br.com.filasaude.service.ProtocoloService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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

    /**
     * Reclassificação manual de prioridade (itens 3.2 / 3.5): exige motivo,
     * registrado em histórico de auditoria imutável.
     */
    @PatchMapping("/{id}/prioridade")
    public ProtocoloResponse alterarPrioridade(@PathVariable Long id, @Valid @RequestBody AlterarPrioridadeRequest request) {
        return protocoloService.alterarPrioridade(id, request);
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

    /**
     * Exportação de dados (item 3.2 do levantamento de requisitos): planilha
     * CSV da fila filtrada, para abrir em Excel/LibreOffice.
     */
    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) CategoriaPrioridade categoria,
            @RequestParam(required = false) StatusProtocolo status,
            @RequestParam(required = false) Long procedimentoId,
            @RequestParam(required = false) Long acsResponsavelId) {

        List<ProtocoloResponse> itens = protocoloService.listarFilaParaExportacao(
                categoria, status, procedimentoId, acsResponsavelId);

        StringBuilder csv = new StringBuilder();
        // BOM UTF-8 para o Excel reconhecer acentuação corretamente
        csv.append('﻿');
        csv.append("Protocolo;Paciente;Procedimento;Unidade de Saude;Categoria;Status;")
                .append("Data Solicitacao;Data Inclusao;Data Prevista;Dias em Espera;Posicao na Fila\n");

        for (ProtocoloResponse p : itens) {
            csv.append(csvSeguro(p.numeroProtocolo())).append(';')
                    .append(csvSeguro(p.nomePaciente())).append(';')
                    .append(csvSeguro(p.nomeProcedimento())).append(';')
                    .append(csvSeguro(p.nomeUnidadeSaude())).append(';')
                    .append(csvSeguro(p.categoriaPrioridade())).append(';')
                    .append(csvSeguro(p.status())).append(';')
                    .append(csvSeguro(dataOuVazio(p.dataSolicitacao()))).append(';')
                    .append(csvSeguro(dataOuVazio(p.dataInclusao()))).append(';')
                    .append(csvSeguro(dataOuVazio(p.dataPrevista()))).append(';')
                    .append(p.diasEmEspera()).append(';')
                    .append(p.posicaoFila() != null ? p.posicaoFila() : "")
                    .append('\n');
        }

        String nomeArquivo = "fila-saude-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String dataOuVazio(LocalDate data) {
        return data != null ? data.toString() : "";
    }

    private static String csvSeguro(String valor) {
        if (valor == null) {
            return "";
        }
        String escapado = valor.replace("\"", "\"\"");
        return "\"" + escapado + "\"";
    }
}
