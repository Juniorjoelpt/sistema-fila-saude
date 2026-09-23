package br.com.filasaude.controller;

import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.dto.cadastro.ProcedimentoRequest;
import br.com.filasaude.repository.ProcedimentoRepository;
import br.com.filasaude.service.AuditoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedimentos")
public class ProcedimentoController {

    private final ProcedimentoRepository procedimentoRepository;
    private final AuditoriaService auditoriaService;

    public ProcedimentoController(ProcedimentoRepository procedimentoRepository, AuditoriaService auditoriaService) {
        this.procedimentoRepository = procedimentoRepository;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Procedimento criar(@Valid @RequestBody ProcedimentoRequest request) {
        Procedimento procedimento = Procedimento.builder()
                .nome(request.nome())
                .tipo(request.tipo())
                .especialidade(request.especialidade())
                .build();
        Procedimento salvo = procedimentoRepository.save(procedimento);
        auditoriaService.registrar("CRIAR_PROCEDIMENTO", "Procedimento", salvo.getId(), "Cadastrado " + salvo.getNome());
        return salvo;
    }

    @GetMapping
    public List<Procedimento> listar() {
        return procedimentoRepository.findByAtivoTrue();
    }
}
