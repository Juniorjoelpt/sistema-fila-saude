package br.com.filasaude.service;

import br.com.filasaude.domain.HistoricoPrioridade;
import br.com.filasaude.domain.HistoricoStatus;
import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.UnidadeSaude;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.domain.enums.CategoriaPrioridade;
import br.com.filasaude.domain.enums.Papel;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.common.PageResponse;
import br.com.filasaude.dto.protocolo.AlterarPrioridadeRequest;
import br.com.filasaude.dto.protocolo.DistribuirVagasRequest;
import br.com.filasaude.dto.protocolo.EtapaAdminResponse;
import br.com.filasaude.dto.protocolo.HistoricoPrioridadeResponse;
import br.com.filasaude.dto.protocolo.HistoricoStatusResponse;
import br.com.filasaude.dto.protocolo.ProtocoloCreateRequest;
import br.com.filasaude.dto.protocolo.ProtocoloDetalheResponse;
import br.com.filasaude.dto.protocolo.ProtocoloResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.HistoricoPrioridadeRepository;
import br.com.filasaude.repository.HistoricoStatusRepository;
import br.com.filasaude.repository.PacienteRepository;
import br.com.filasaude.repository.ProcedimentoRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.repository.UnidadeSaudeRepository;
import br.com.filasaude.repository.UsuarioRepository;
import br.com.filasaude.specification.ProtocoloSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProtocoloService {

    /**
     * Transições de status permitidas (bug de revisão: {@code mudarStatus} não
     * tinha nenhuma máquina de estados, então era possível fazer, por exemplo,
     * CONCLUIDO -> AGUARDANDO ou CANCELADO -> AGENDADO livremente, quebrando a
     * integridade do histórico e a posição de outros pacientes na fila).
     * CONCLUIDO e CANCELADO são estados terminais -- não há transição de saída
     * deles aqui; uma eventual "reabertura" ficaria para uma ação administrativa
     * própria e auditada, não para uma troca de status qualquer.
     */
    private static final Map<StatusProtocolo, Set<StatusProtocolo>> TRANSICOES_PERMITIDAS = new EnumMap<>(StatusProtocolo.class);
    static {
        TRANSICOES_PERMITIDAS.put(StatusProtocolo.AGUARDANDO, EnumSet.of(StatusProtocolo.AGENDADO, StatusProtocolo.CANCELADO));
        TRANSICOES_PERMITIDAS.put(StatusProtocolo.AGENDADO, EnumSet.of(StatusProtocolo.EM_ANDAMENTO, StatusProtocolo.AGUARDANDO, StatusProtocolo.CANCELADO));
        TRANSICOES_PERMITIDAS.put(StatusProtocolo.EM_ANDAMENTO, EnumSet.of(StatusProtocolo.CONCLUIDO, StatusProtocolo.CANCELADO));
        TRANSICOES_PERMITIDAS.put(StatusProtocolo.CONCLUIDO, EnumSet.noneOf(StatusProtocolo.class));
        TRANSICOES_PERMITIDAS.put(StatusProtocolo.CANCELADO, EnumSet.noneOf(StatusProtocolo.class));
    }

    private final ProtocoloRepository protocoloRepository;
    private final PacienteRepository pacienteRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final UnidadeSaudeRepository unidadeSaudeRepository;
    private final HistoricoStatusRepository historicoStatusRepository;
    private final HistoricoPrioridadeRepository historicoPrioridadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final EtapasPadraoFactory etapasPadraoFactory;
    private final FilaPriorizacaoService filaPriorizacaoService;
    private final NotificacaoEmailService notificacaoEmailService;
    private final AuditoriaService auditoriaService;

    public ProtocoloService(ProtocoloRepository protocoloRepository,
                             PacienteRepository pacienteRepository,
                             ProcedimentoRepository procedimentoRepository,
                             UnidadeSaudeRepository unidadeSaudeRepository,
                             HistoricoStatusRepository historicoStatusRepository,
                             HistoricoPrioridadeRepository historicoPrioridadeRepository,
                             UsuarioRepository usuarioRepository,
                             EtapasPadraoFactory etapasPadraoFactory,
                             FilaPriorizacaoService filaPriorizacaoService,
                             NotificacaoEmailService notificacaoEmailService,
                             AuditoriaService auditoriaService) {
        this.protocoloRepository = protocoloRepository;
        this.pacienteRepository = pacienteRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.unidadeSaudeRepository = unidadeSaudeRepository;
        this.historicoStatusRepository = historicoStatusRepository;
        this.historicoPrioridadeRepository = historicoPrioridadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.etapasPadraoFactory = etapasPadraoFactory;
        this.filaPriorizacaoService = filaPriorizacaoService;
        this.notificacaoEmailService = notificacaoEmailService;
        this.auditoriaService = auditoriaService;
    }

    public ProtocoloResponse criar(ProtocoloCreateRequest request) {
        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + request.pacienteId()));
        Procedimento procedimento = procedimentoRepository.findById(request.procedimentoId())
                .orElseThrow(() -> new ResourceNotFoundException("Procedimento não encontrado: " + request.procedimentoId()));
        UnidadeSaude unidade = null;
        if (request.unidadeSaudeId() != null) {
            unidade = unidadeSaudeRepository.findById(request.unidadeSaudeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unidade de saúde não encontrada: " + request.unidadeSaudeId()));
        }

        if (request.categoriaPrioridade() == CategoriaPrioridade.JUDICIAL
                && (request.processoJudicial() == null || request.processoJudicial().isBlank())) {
            throw new IllegalStateException(
                    "Protocolos na categoria Judicial exigem o número do processo/mandado (item 3.2 — Vínculo Judicial)");
        }
        validarCriterioPrioridade(request.categoriaPrioridade(), paciente);

        Protocolo protocolo = Protocolo.builder()
                // Placeholder temporário (cabe no varchar(30) da coluna): o número final
                // depende do ID gerado pelo banco (ver gerarNumeroProtocolo), que só
                // existe após o save. Precisa ser único porque a coluna tem constraint
                // UNIQUE -- nanoTime em hex é suficiente para não colidir na janela
                // entre os dois saves desta mesma transação.
                .numeroProtocolo("TMP-" + Long.toHexString(System.nanoTime()))
                .paciente(paciente)
                .procedimento(procedimento)
                .unidadeSaude(unidade)
                .categoriaPrioridade(request.categoriaPrioridade())
                .processoJudicial(request.processoJudicial())
                .dataSolicitacao(request.dataSolicitacao())
                .status(StatusProtocolo.AGUARDANDO)
                .build();

        protocolo.setEtapas(etapasPadraoFactory.gerarPara(protocolo, procedimento.getTipo()));

        Protocolo salvo = protocoloRepository.save(protocolo);
        salvo.setNumeroProtocolo(gerarNumeroProtocolo(salvo.getId()));
        salvo = protocoloRepository.save(salvo);
        registrarHistorico(salvo, null, StatusProtocolo.AGUARDANDO, "Protocolo criado e incluído na fila");
        auditoriaService.registrar("CRIAR_PROTOCOLO", "Protocolo", salvo.getId(),
                "Protocolo " + salvo.getNumeroProtocolo() + " criado para " + salvo.getPaciente().getNome());

        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProtocoloResponse> listarFila(CategoriaPrioridade categoria, StatusProtocolo status,
                                                        Long procedimentoId, Long acsResponsavelId,
                                                        int page, int size) {
        List<Protocolo> ordenados = buscarFilaOrdenada(categoria, status, procedimentoId, acsResponsavelId);

        int inicio = Math.min(page * size, ordenados.size());
        int fim = Math.min(inicio + size, ordenados.size());
        List<ProtocoloResponse> pagina = ordenados.subList(inicio, fim).stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.de(pagina, page, size, ordenados.size());
    }

    /**
     * Exportação de dados (item 3.2): retorna toda a fila filtrada, sem
     * paginação, na mesma ordenação exibida na tela — usada para gerar a
     * planilha (CSV) que o operador baixa.
     */
    @Transactional(readOnly = true)
    public List<ProtocoloResponse> listarFilaParaExportacao(CategoriaPrioridade categoria, StatusProtocolo status,
                                                              Long procedimentoId, Long acsResponsavelId) {
        return buscarFilaOrdenada(categoria, status, procedimentoId, acsResponsavelId).stream()
                .map(this::toResponse)
                .toList();
    }

    private List<Protocolo> buscarFilaOrdenada(CategoriaPrioridade categoria, StatusProtocolo status,
                                                 Long procedimentoId, Long acsResponsavelId) {
        // ACS (item 2 do levantamento de requisitos: "cadastra e acompanha
        // pacientes da sua área") só pode ver a fila dos próprios pacientes --
        // o filtro é forçado no backend, ignorando qualquer valor vindo do
        // cliente, para não poder ser contornado.
        Long acsEfetivo = acsResponsavelId;
        Optional<Usuario> usuarioLogado = usuarioLogado();
        if (usuarioLogado.isPresent() && usuarioLogado.get().getPapel() == Papel.ACS) {
            acsEfetivo = usuarioLogado.get().getId();
        }

        Specification<Protocolo> spec = Specification
                .where(ProtocoloSpecifications.comCategoria(categoria))
                .and(ProtocoloSpecifications.comStatus(status))
                .and(ProtocoloSpecifications.comProcedimento(procedimentoId))
                .and(ProtocoloSpecifications.comAcsResponsavel(acsEfetivo));

        List<Protocolo> todos = protocoloRepository.findAll(spec);

        Comparator<Protocolo> ordenacao = Comparator
                .<Protocolo>comparingInt(p -> p.getCategoriaPrioridade().getPeso())
                .thenComparing(Protocolo::getDataInclusao);
        return todos.stream().sorted(ordenacao).toList();
    }

    @Transactional(readOnly = true)
    public ProtocoloDetalheResponse buscarDetalhe(Long id) {
        Protocolo protocolo = buscarOuFalhar(id);

        // ACS só pode ver o detalhe de protocolos dos próprios pacientes (ver
        // buscarFilaOrdenada); tratamos como "não encontrado" em vez de
        // "acesso negado" para não confirmar a existência do protocolo.
        Optional<Usuario> usuarioLogado = usuarioLogado();
        if (usuarioLogado.isPresent() && usuarioLogado.get().getPapel() == Papel.ACS) {
            Usuario acsDoPaciente = protocolo.getPaciente().getAcsResponsavel();
            if (acsDoPaciente == null || !acsDoPaciente.getId().equals(usuarioLogado.get().getId())) {
                throw new ResourceNotFoundException("Protocolo não encontrado: " + id);
            }
        }

        ProtocoloResponse resumo = toResponse(protocolo);

        List<EtapaAdminResponse> etapas = protocolo.getEtapas().stream()
                .map(EtapaAdminResponse::de)
                .toList();

        List<HistoricoStatusResponse> historico = historicoStatusRepository
                .findByProtocoloIdOrderByCriadoEmAsc(id).stream()
                .map(HistoricoStatusResponse::de)
                .toList();

        List<HistoricoPrioridadeResponse> historicoPrioridade = historicoPrioridadeRepository
                .findByProtocoloIdOrderByCriadoEmAsc(id).stream()
                .map(HistoricoPrioridadeResponse::de)
                .toList();

        return ProtocoloDetalheResponse.de(resumo, etapas, historico, historicoPrioridade);
    }

    /**
     * Reclassificação manual de prioridade de um protocolo (itens 3.2 e 3.5
     * do levantamento de requisitos). Exige motivo, o que sustenta a
     * auditoria contra questionamentos éticos/judiciais ao gestor -- o
     * mesmo cuidado dado ao histórico de status.
     */
    public ProtocoloResponse alterarPrioridade(Long protocoloId, AlterarPrioridadeRequest request) {
        Protocolo protocolo = buscarOuFalhar(protocoloId);
        CategoriaPrioridade prioridadeAnterior = protocolo.getCategoriaPrioridade();

        if (request.novaCategoria() == CategoriaPrioridade.JUDICIAL
                && (request.processoJudicial() == null || request.processoJudicial().isBlank())) {
            throw new IllegalStateException(
                    "Número do processo judicial é obrigatório para a categoria Judicial");
        }
        validarCriterioPrioridade(request.novaCategoria(), protocolo.getPaciente());

        protocolo.setCategoriaPrioridade(request.novaCategoria());
        if (request.novaCategoria() == CategoriaPrioridade.JUDICIAL) {
            protocolo.setProcessoJudicial(request.processoJudicial());
        }
        protocolo.setAtualizadoEm(java.time.LocalDateTime.now());
        protocoloRepository.save(protocolo);

        if (prioridadeAnterior != request.novaCategoria()) {
            historicoPrioridadeRepository.save(HistoricoPrioridade.builder()
                    .protocolo(protocolo)
                    .prioridadeAnterior(prioridadeAnterior)
                    .prioridadeNova(request.novaCategoria())
                    .usuario(usuarioLogado().orElse(null))
                    .motivo(request.motivo())
                    .build());
            auditoriaService.registrar("ALTERAR_PRIORIDADE_PROTOCOLO", "Protocolo", protocolo.getId(),
                    "Protocolo " + protocolo.getNumeroProtocolo() + ": " + prioridadeAnterior
                            + " -> " + request.novaCategoria() + " (motivo: " + request.motivo() + ")");
        }

        return toResponse(protocolo);
    }

    private Optional<Usuario> usuarioLogado() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(a -> a.getName())
                .flatMap(usuarioRepository::findByEmailIgnoreCase);
    }

    public ProtocoloResponse mudarStatus(Long protocoloId, StatusProtocolo novoStatus, String observacao) {
        Protocolo protocolo = buscarOuFalhar(protocoloId);
        StatusProtocolo statusAnterior = protocolo.getStatus();

        validarTransicao(statusAnterior, novoStatus);

        protocolo.setStatus(novoStatus);
        protocolo.setAtualizadoEm(java.time.LocalDateTime.now());
        protocoloRepository.save(protocolo);

        registrarHistorico(protocolo, statusAnterior, novoStatus, observacao);
        auditoriaService.registrar("MUDAR_STATUS_PROTOCOLO", "Protocolo", protocolo.getId(),
                "Protocolo " + protocolo.getNumeroProtocolo() + ": " + statusAnterior + " -> " + novoStatus);

        // Notificação por e-mail ao cidadão em mudança de status — decisão registrada
        // no levantamento de requisitos (itens 3.1 / 6.1: notificação já no MVP).
        notificacaoEmailService.notificarMudancaStatus(protocolo);

        return toResponse(protocolo);
    }

    /**
     * Valida se a transição de status é permitida (ver {@link #TRANSICOES_PERMITIDAS}).
     * Manter o mesmo status é sempre um no-op tolerado (o frontend já desabilita o
     * botão nesse caso, mas o backend não deve depender só disso).
     */
    private void validarTransicao(StatusProtocolo statusAnterior, StatusProtocolo novoStatus) {
        if (statusAnterior == novoStatus) {
            return;
        }
        Set<StatusProtocolo> permitidos = TRANSICOES_PERMITIDAS.getOrDefault(statusAnterior, EnumSet.noneOf(StatusProtocolo.class));
        if (!permitidos.contains(novoStatus)) {
            throw new IllegalStateException(
                    "Transição de status não permitida: %s -> %s".formatted(statusAnterior, novoStatus));
        }
    }

    public List<ProtocoloResponse> distribuirVagas(DistribuirVagasRequest request) {
        List<Protocolo> protocolos = protocoloRepository.findAllById(request.protocoloIds());
        if (protocolos.size() != request.protocoloIds().size()) {
            throw new ResourceNotFoundException("Um ou mais protocolos informados não foram encontrados");
        }

        // Bug de revisão: antes não checava o status atual, então um protocolo já
        // CANCELADO/CONCLUIDO incluído no lote virava AGENDADO silenciosamente,
        // reabrindo algo já encerrado. Só protocolos aguardando podem receber vaga.
        List<Protocolo> foraDoStatusEsperado = protocolos.stream()
                .filter(p -> p.getStatus() != StatusProtocolo.AGUARDANDO)
                .toList();
        if (!foraDoStatusEsperado.isEmpty()) {
            String numeros = foraDoStatusEsperado.stream()
                    .map(p -> p.getNumeroProtocolo() + " (" + p.getStatus() + ")")
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException(
                    "Só é possível distribuir vaga para protocolos aguardando atendimento. "
                            + "Remova da seleção: " + numeros);
        }

        List<ProtocoloResponse> resultado = protocolos.stream().map(protocolo -> {
            StatusProtocolo statusAnterior = protocolo.getStatus();
            protocolo.setStatus(StatusProtocolo.AGENDADO);
            protocolo.setDataPrevista(request.dataPrevista());
            protocolo.setAtualizadoEm(java.time.LocalDateTime.now());
            protocoloRepository.save(protocolo);
            registrarHistorico(protocolo, statusAnterior, StatusProtocolo.AGENDADO, "Vaga distribuída em lote");
            notificacaoEmailService.notificarMudancaStatus(protocolo);
            return toResponse(protocolo);
        }).toList();

        auditoriaService.registrar("DISTRIBUIR_VAGAS", "Protocolo", null,
                protocolos.size() + " protocolo(s) agendado(s) em lote para " + request.dataPrevista());

        return resultado;
    }

    public ProtocoloResponse marcarEtapa(Long protocoloId, Long etapaId, br.com.filasaude.domain.enums.StatusEtapa novoStatus) {
        Protocolo protocolo = buscarOuFalhar(protocoloId);
        protocolo.getEtapas().stream()
                .filter(e -> e.getId().equals(etapaId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Etapa não encontrada: " + etapaId))
                .setStatus(novoStatus);

        if (novoStatus == br.com.filasaude.domain.enums.StatusEtapa.REALIZADO) {
            protocolo.getEtapas().stream()
                    .filter(e -> e.getId().equals(etapaId))
                    .findFirst()
                    .ifPresent(e -> e.setDataRealizacao(LocalDate.now()));
        }

        protocolo.setAtualizadoEm(java.time.LocalDateTime.now());
        return toResponse(protocoloRepository.save(protocolo));
    }

    /**
     * Gap de revisão corrigido: as categorias ESPECIAL (80+) e LEGAL (60+, PCD,
     * gestante -- item 3.2 do levantamento de requisitos) não tinham nenhuma
     * checagem real; o operador escolhia livremente no formulário e o backend
     * aceitava sem cruzar com idade/PCD/gestante do paciente. Sem data de
     * nascimento cadastrada, o critério por idade não pode ser confirmado --
     * tratado como não atendido (falha aberta seria pior aqui).
     */
    private void validarCriterioPrioridade(CategoriaPrioridade categoria, Paciente paciente) {
        if (categoria == CategoriaPrioridade.ESPECIAL) {
            if (!temIdadeMinima(paciente, 80)) {
                throw new IllegalStateException(
                        "Categoria Especial (80+) exige paciente com 80 anos ou mais e data de nascimento cadastrada.");
            }
        } else if (categoria == CategoriaPrioridade.LEGAL) {
            boolean elegivel = temIdadeMinima(paciente, 60) || paciente.isPcd() || paciente.isGestante();
            if (!elegivel) {
                throw new IllegalStateException(
                        "Categoria Legal exige paciente com 60 anos ou mais (com data de nascimento cadastrada), "
                                + "PCD ou gestante -- marque a condição no cadastro do paciente.");
            }
        }
    }

    private boolean temIdadeMinima(Paciente paciente, int anos) {
        if (paciente.getDataNascimento() == null) {
            return false;
        }
        return java.time.Period.between(paciente.getDataNascimento(), LocalDate.now()).getYears() >= anos;
    }

    private Protocolo buscarOuFalhar(Long id) {
        return protocoloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Protocolo não encontrado: " + id));
    }

    private void registrarHistorico(Protocolo protocolo, StatusProtocolo anterior, StatusProtocolo novo, String observacao) {
        String emailUsuarioLogado = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(a -> a.getName())
                .orElse(null);

        historicoStatusRepository.save(HistoricoStatus.builder()
                .protocolo(protocolo)
                .statusAnterior(anterior)
                .statusNovo(novo)
                .observacao((observacao != null ? observacao : "") +
                        (emailUsuarioLogado != null ? " [por: " + emailUsuarioLogado + "]" : ""))
                .build());
    }

    /**
     * Bug de revisão corrigido: a versão anterior usava {@code count() + 1} como
     * sequencial, sem lock nem transação serializável -- duas criações de
     * protocolo concorrentes liam a mesma contagem antes de qualquer save
     * completar e geravam o MESMO número, e a segunda gravação estourava a
     * constraint UNIQUE da coluna como um 500 genérico (ver também o handler de
     * DataIntegrityViolationException no GlobalExceptionHandler).
     *
     * Agora o sequencial é o próprio ID gerado pelo banco (AUTO_INCREMENT), que é
     * atômico por natureza -- não existem dois protocolos com o mesmo ID. O
     * número deixa de "reiniciar" a cada ano (é global, crescente), troca aceita
     * em favor de nunca colidir.
     */
    private String gerarNumeroProtocolo(Long id) {
        int ano = Year.now().getValue();
        return "PROT-%d-%06d".formatted(ano, id);
    }

    private ProtocoloResponse toResponse(Protocolo protocolo) {
        Integer posicao = protocolo.getStatus() == StatusProtocolo.AGUARDANDO
                ? filaPriorizacaoService.posicaoNaFila(protocolo.getId()).orElse(null)
                : null;
        return ProtocoloResponse.de(protocolo, posicao);
    }
}
