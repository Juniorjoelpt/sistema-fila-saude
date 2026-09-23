package br.com.filasaude.controller;

import br.com.filasaude.domain.UnidadeSaude;
import br.com.filasaude.dto.cadastro.UnidadeSaudeRequest;
import br.com.filasaude.repository.UnidadeSaudeRepository;
import br.com.filasaude.service.AuditoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unidades")
public class UnidadeSaudeController {

    private final UnidadeSaudeRepository unidadeSaudeRepository;
    private final AuditoriaService auditoriaService;

    public UnidadeSaudeController(UnidadeSaudeRepository unidadeSaudeRepository, AuditoriaService auditoriaService) {
        this.unidadeSaudeRepository = unidadeSaudeRepository;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnidadeSaude criar(@Valid @RequestBody UnidadeSaudeRequest request) {
        UnidadeSaude unidade = UnidadeSaude.builder()
                .nome(request.nome())
                .endereco(request.endereco())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .codigoCnes(request.codigoCnes())
                .build();
        UnidadeSaude salva = unidadeSaudeRepository.save(unidade);
        auditoriaService.registrar("CRIAR_UNIDADE", "UnidadeSaude", salva.getId(), "Cadastrada " + salva.getNome());
        return salva;
    }

    @GetMapping
    public List<UnidadeSaude> listar() {
        return unidadeSaudeRepository.findAll();
    }
}
