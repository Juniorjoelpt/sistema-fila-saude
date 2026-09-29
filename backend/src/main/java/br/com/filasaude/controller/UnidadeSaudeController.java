package br.com.filasaude.controller;

import br.com.filasaude.domain.UnidadeSaude;
import br.com.filasaude.dto.cadastro.UnidadeSaudeRequest;
import br.com.filasaude.exception.ResourceNotFoundException;
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
        // Bug alto de revisão corrigido: nenhuma checagem de nome ou CNES
        // duplicado -- era possível cadastrar a mesma unidade de saúde duas
        // vezes (o código CNES é, na prática, um identificador único do
        // Ministério da Saúde).
        verificarDuplicidade(request, null);

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

    // Bug alto de revisão corrigido: CRUD incompleto -- só existiam POST/GET;
    // campo `ativo` nunca era escrito por nenhum endpoint.
    @PutMapping("/{id}")
    public UnidadeSaude atualizar(@PathVariable Long id, @Valid @RequestBody UnidadeSaudeRequest request) {
        UnidadeSaude unidade = unidadeSaudeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade de saúde não encontrada: " + id));

        verificarDuplicidade(request, id);

        unidade.setNome(request.nome());
        unidade.setEndereco(request.endereco());
        unidade.setLatitude(request.latitude());
        unidade.setLongitude(request.longitude());
        unidade.setCodigoCnes(request.codigoCnes());
        UnidadeSaude salva = unidadeSaudeRepository.save(unidade);
        auditoriaService.registrar("ATUALIZAR_UNIDADE", "UnidadeSaude", salva.getId(), "Atualizada " + salva.getNome());
        return salva;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable Long id) {
        UnidadeSaude unidade = unidadeSaudeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade de saúde não encontrada: " + id));
        unidade.setAtivo(false);
        unidadeSaudeRepository.save(unidade);
        auditoriaService.registrar("DESATIVAR_UNIDADE", "UnidadeSaude", id, "Desativada " + unidade.getNome());
    }

    private void verificarDuplicidade(UnidadeSaudeRequest request, Long idAtual) {
        unidadeSaudeRepository.findByNomeIgnoreCase(request.nome())
                .filter(outra -> idAtual == null || !outra.getId().equals(idAtual))
                .ifPresent(u -> {
                    throw new IllegalStateException("Já existe uma unidade de saúde cadastrada com este nome: " + request.nome());
                });
        if (request.codigoCnes() != null && !request.codigoCnes().isBlank()) {
            unidadeSaudeRepository.findByCodigoCnes(request.codigoCnes())
                    .filter(outra -> idAtual == null || !outra.getId().equals(idAtual))
                    .ifPresent(u -> {
                        throw new IllegalStateException("Já existe uma unidade de saúde cadastrada com este código CNES: " + request.codigoCnes());
                    });
        }
    }
}
