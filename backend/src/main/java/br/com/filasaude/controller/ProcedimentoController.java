package br.com.filasaude.controller;

import br.com.filasaude.domain.Especialidade;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.dto.cadastro.ProcedimentoRequest;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.EspecialidadeRepository;
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
    private final EspecialidadeRepository especialidadeRepository;
    private final AuditoriaService auditoriaService;

    public ProcedimentoController(ProcedimentoRepository procedimentoRepository,
                                   EspecialidadeRepository especialidadeRepository,
                                   AuditoriaService auditoriaService) {
        this.procedimentoRepository = procedimentoRepository;
        this.especialidadeRepository = especialidadeRepository;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Procedimento criar(@Valid @RequestBody ProcedimentoRequest request) {
        // Bug alto de revisão corrigido: não havia nenhuma checagem de nome
        // duplicado -- era possível cadastrar "Consulta Cardiologia" duas
        // vezes, gerando registros ambíguos na fila e na importação CSV
        // (que casa procedimento pelo nome).
        procedimentoRepository.findByNomeIgnoreCase(request.nome()).ifPresent(p -> {
            throw new IllegalStateException("Já existe um procedimento cadastrado com este nome: " + request.nome());
        });
        validarEspecialidadeCadastrada(request.especialidade());

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

    // Bug alto de revisão corrigido: CRUD incompleto -- só existiam POST/GET.
    // Não havia como corrigir um cadastro errado nem "desativar" um
    // procedimento descontinuado (o campo `ativo` existia na entidade mas
    // nunca era escrito por nenhum endpoint).
    @PutMapping("/{id}")
    public Procedimento atualizar(@PathVariable Long id, @Valid @RequestBody ProcedimentoRequest request) {
        Procedimento procedimento = procedimentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimento não encontrado: " + id));

        procedimentoRepository.findByNomeIgnoreCase(request.nome())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(p -> {
                    throw new IllegalStateException("Já existe um procedimento cadastrado com este nome: " + request.nome());
                });
        validarEspecialidadeCadastrada(request.especialidade());

        procedimento.setNome(request.nome());
        procedimento.setTipo(request.tipo());
        procedimento.setEspecialidade(request.especialidade());
        Procedimento salvo = procedimentoRepository.save(procedimento);
        auditoriaService.registrar("ATUALIZAR_PROCEDIMENTO", "Procedimento", salvo.getId(), "Atualizado " + salvo.getNome());
        return salvo;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable Long id) {
        Procedimento procedimento = procedimentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimento não encontrado: " + id));
        // Exclusão lógica (ativo=false), não física: protocolos antigos continuam
        // referenciando este procedimento e não podem ficar com FK quebrada.
        procedimento.setAtivo(false);
        procedimentoRepository.save(procedimento);
        auditoriaService.registrar("DESATIVAR_PROCEDIMENTO", "Procedimento", id, "Desativado " + procedimento.getNome());
    }

    /**
     * Gap de revisão corrigido: a especialidade era texto livre, sem
     * nenhuma validação contra o cadastro (ver Especialidade) -- um erro de
     * digitação aqui quebra silenciosamente o casamento por string com
     * Cotas/Agenda/SLA. Null continua permitido (especialidade é opcional).
     */
    private void validarEspecialidadeCadastrada(String especialidade) {
        if (especialidade == null) {
            return;
        }
        especialidadeRepository.findByNomeIgnoreCase(especialidade)
                .filter(Especialidade::isAtivo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Especialidade não cadastrada ou inativa: " + especialidade + " -- cadastre-a antes em \"Cadastros > Especialidades\""));
    }
}
