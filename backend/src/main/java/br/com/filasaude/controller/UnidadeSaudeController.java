package br.com.filasaude.controller;

import br.com.filasaude.domain.UnidadeSaude;
import br.com.filasaude.dto.cadastro.UnidadeSaudeRequest;
import br.com.filasaude.repository.UnidadeSaudeRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unidades")
public class UnidadeSaudeController {

    private final UnidadeSaudeRepository unidadeSaudeRepository;

    public UnidadeSaudeController(UnidadeSaudeRepository unidadeSaudeRepository) {
        this.unidadeSaudeRepository = unidadeSaudeRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnidadeSaude criar(@Valid @RequestBody UnidadeSaudeRequest request) {
        UnidadeSaude unidade = UnidadeSaude.builder()
                .nome(request.nome())
                .endereco(request.endereco())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();
        return unidadeSaudeRepository.save(unidade);
    }

    @GetMapping
    public List<UnidadeSaude> listar() {
        return unidadeSaudeRepository.findAll();
    }
}
