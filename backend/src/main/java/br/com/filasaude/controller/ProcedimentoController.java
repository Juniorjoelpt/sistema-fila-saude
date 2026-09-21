package br.com.filasaude.controller;

import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.dto.cadastro.ProcedimentoRequest;
import br.com.filasaude.repository.ProcedimentoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedimentos")
public class ProcedimentoController {

    private final ProcedimentoRepository procedimentoRepository;

    public ProcedimentoController(ProcedimentoRepository procedimentoRepository) {
        this.procedimentoRepository = procedimentoRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Procedimento criar(@Valid @RequestBody ProcedimentoRequest request) {
        Procedimento procedimento = Procedimento.builder()
                .nome(request.nome())
                .tipo(request.tipo())
                .especialidade(request.especialidade())
                .build();
        return procedimentoRepository.save(procedimento);
    }

    @GetMapping
    public List<Procedimento> listar() {
        return procedimentoRepository.findByAtivoTrue();
    }
}
