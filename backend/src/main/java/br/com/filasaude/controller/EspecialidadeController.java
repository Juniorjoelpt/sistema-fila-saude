package br.com.filasaude.controller;

import br.com.filasaude.domain.Especialidade;
import br.com.filasaude.dto.cadastro.EspecialidadeRequest;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.EspecialidadeRepository;
import br.com.filasaude.service.AuditoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cadastro de especialidades (gap de revisão corrigido: ver Especialidade).
 * Mesmo padrão de Procedimentos: Regulador/Admin administram, leitura
 * também liberada ao ACS (ver SecurityConfig) -- embora hoje o ACS não
 * monte nenhum select de especialidade diretamente.
 */
@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadeController {

    private final EspecialidadeRepository especialidadeRepository;
    private final AuditoriaService auditoriaService;

    public EspecialidadeController(EspecialidadeRepository especialidadeRepository, AuditoriaService auditoriaService) {
        this.especialidadeRepository = especialidadeRepository;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<Especialidade> listar() {
        return especialidadeRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Especialidade criar(@Valid @RequestBody EspecialidadeRequest request) {
        especialidadeRepository.findByNomeIgnoreCase(request.nome()).ifPresent(e -> {
            throw new IllegalStateException("Já existe uma especialidade cadastrada com este nome: " + request.nome());
        });

        Especialidade salvo = especialidadeRepository.save(Especialidade.builder().nome(request.nome()).build());
        auditoriaService.registrar("CRIAR_ESPECIALIDADE", "Especialidade", salvo.getId(), "Cadastrada " + salvo.getNome());
        return salvo;
    }

    @PutMapping("/{id}")
    public Especialidade atualizar(@PathVariable Long id, @Valid @RequestBody EspecialidadeRequest request) {
        Especialidade especialidade = especialidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidade não encontrada: " + id));

        especialidadeRepository.findByNomeIgnoreCase(request.nome())
                .filter(outra -> !outra.getId().equals(id))
                .ifPresent(e -> {
                    throw new IllegalStateException("Já existe uma especialidade cadastrada com este nome: " + request.nome());
                });

        // Nota: renomear aqui NÃO atualiza o texto já gravado em
        // Procedimento/Cota/HorarioAgenda/SlaConfiguracao -- esses continuam
        // com o nome antigo até serem editados individualmente. Preferível a
        // uma renomeação em cascata silenciosa sobre dados de outras telas.
        especialidade.setNome(request.nome());
        especialidade.setAtualizadoEm(LocalDateTime.now());
        Especialidade salvo = especialidadeRepository.save(especialidade);
        auditoriaService.registrar("ATUALIZAR_ESPECIALIDADE", "Especialidade", salvo.getId(), "Renomeada para " + salvo.getNome());
        return salvo;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable Long id) {
        Especialidade especialidade = especialidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidade não encontrada: " + id));
        // Exclusão lógica, como em Procedimento: registros antigos (procedimentos,
        // cotas, horários, configurações de SLA) continuam com o nome gravado
        // como texto, não como FK -- não há nada para "quebrar" ao desativar,
        // só deixa de aparecer nos selects para novos cadastros.
        especialidade.setAtivo(false);
        especialidade.setAtualizadoEm(LocalDateTime.now());
        especialidadeRepository.save(especialidade);
        auditoriaService.registrar("DESATIVAR_ESPECIALIDADE", "Especialidade", id, "Desativada " + especialidade.getNome());
    }
}
