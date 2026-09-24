package br.com.filasaude.controller;

import br.com.filasaude.dto.agenda.HorarioAgendaCreateRequest;
import br.com.filasaude.dto.agenda.HorarioAgendaLoteRequest;
import br.com.filasaude.dto.agenda.HorarioAgendaResponse;
import br.com.filasaude.service.HorarioAgendaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Gestão da agenda de horários reais (unidade + especialidade + data + hora,
 * com controle de vaga) -- restrito a Regulador/Admin, mesmo nível de acesso
 * de Cotas (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/horarios-agenda")
public class HorarioAgendaController {

    private final HorarioAgendaService horarioAgendaService;

    public HorarioAgendaController(HorarioAgendaService horarioAgendaService) {
        this.horarioAgendaService = horarioAgendaService;
    }

    @GetMapping
    public List<HorarioAgendaResponse> listar(
            @RequestParam Long unidadeSaudeId,
            @RequestParam(required = false) String especialidade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return horarioAgendaService.listar(unidadeSaudeId, especialidade, dataInicio, dataFim);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HorarioAgendaResponse criar(@Valid @RequestBody HorarioAgendaCreateRequest request) {
        return horarioAgendaService.criar(request);
    }

    @PostMapping("/lote")
    public Map<String, Integer> gerarLote(@Valid @RequestBody HorarioAgendaLoteRequest request) {
        int criados = horarioAgendaService.gerarLote(request);
        return Map.of("criados", criados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        horarioAgendaService.remover(id);
    }
}
