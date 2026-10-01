package br.com.filasaude.controller;

import br.com.filasaude.dto.paciente.AtribuirAcsRequest;
import br.com.filasaude.dto.paciente.PacienteDuplicadoResponse;
import br.com.filasaude.dto.paciente.PacienteRequest;
import br.com.filasaude.dto.paciente.PacienteResponse;
import br.com.filasaude.service.DuplicidadePacienteService;
import br.com.filasaude.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;
    private final DuplicidadePacienteService duplicidadePacienteService;

    public PacienteController(PacienteService pacienteService, DuplicidadePacienteService duplicidadePacienteService) {
        this.pacienteService = pacienteService;
        this.duplicidadePacienteService = duplicidadePacienteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteResponse cadastrar(@Valid @RequestBody PacienteRequest request) {
        return pacienteService.cadastrar(request);
    }

    /**
     * Item do roadmap "detecção de duplicidade fuzzy de paciente": consultado
     * pelo frontend ANTES de enviar o cadastro (ver NovoProtocolo.tsx), para
     * avisar o operador sem bloquear nada -- a decisão final é dele.
     */
    @GetMapping("/possiveis-duplicados")
    public List<PacienteDuplicadoResponse> possiveisDuplicados(
            @RequestParam String nome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascimento,
            @RequestParam(required = false) String telefone,
            @RequestParam(required = false) Long ignorarId) {
        return duplicidadePacienteService.buscarPossiveisDuplicados(nome, dataNascimento, telefone, ignorarId);
    }

    @GetMapping
    public List<PacienteResponse> listar() {
        return pacienteService.listar();
    }

    @GetMapping("/{id}")
    public PacienteResponse buscar(@PathVariable Long id) {
        return pacienteService.buscarPorId(id);
    }

    @PatchMapping("/{id}/acs-responsavel")
    public PacienteResponse atribuirAcs(@PathVariable Long id, @RequestBody AtribuirAcsRequest request) {
        return pacienteService.atribuirAcsResponsavel(id, request.acsResponsavelId());
    }

    @PutMapping("/{id}")
    public PacienteResponse atualizar(@PathVariable Long id, @Valid @RequestBody PacienteRequest request) {
        return pacienteService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        pacienteService.excluir(id);
    }
}
