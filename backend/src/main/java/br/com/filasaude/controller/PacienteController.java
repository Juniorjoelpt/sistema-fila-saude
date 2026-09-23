package br.com.filasaude.controller;

import br.com.filasaude.dto.paciente.AtribuirAcsRequest;
import br.com.filasaude.dto.paciente.PacienteRequest;
import br.com.filasaude.dto.paciente.PacienteResponse;
import br.com.filasaude.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteResponse cadastrar(@Valid @RequestBody PacienteRequest request) {
        return pacienteService.cadastrar(request);
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
}
