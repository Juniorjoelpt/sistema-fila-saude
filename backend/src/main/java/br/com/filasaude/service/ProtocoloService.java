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
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProtocoloService {

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

        Protocolo protocolo = Protocolo.builder()
                .numeroProtocolo(gerarNumeroProtocolo())
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

        protocolo.setStatus(novoStatus);
        protocolo.setAtualizadoEm(java.time.LocalDateTime.now());
        protocoloRepository.save(protocolo);

        registrarHistorico(protocolo, statusAnterior, novoStatus, observacao);

        // Notificação por e-mail ao cidadão em mudança de status — decisão registrada
        // no levantamento de requisitos (itens 3.1 / 6.1: notificação já no MVP).
        notificacaoEmailService.notificarMudancaStatus(protocolo);

        return toResponse(protocolo);
    }

    public List<ProtocoloResponse> distribuirVagas(DistribuirVagasRequest request) {
        List<Protocolo> protocolos = protocoloRepository.findAllById(request.protocoloIds());
        if (protocolos.size() != request.protocoloIds().size()) {
            throw new ResourceNotFoundException("Um ou mais protocolos informados não foram encontrados");
        }

        return protocolos.stream().map(protocolo -> {
            StatusProtocolo statusAnterior = protocolo.getStatus();
            protocolo.setStatus(StatusProtocolo.AGENDADO);
            protocolo.setDataPrevista(request.dataPrevista());
            protocolo.setAtualizadoEm(java.time.LocalDateTime.now());
            protocoloRepository.save(protocolo);
            registrarHistorico(protocolo, statusAnterior, StatusProtocolo.AGENDADO, "Vaga distribuída em lote");
            notificacaoEmailService.notificarMudancaStatus(protocolo);
            return toResponse(protocolo);
        }).toList();
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

    private String gerarNumeroProtocolo() {
        int ano = Year.now().getValue();
        long sequencial = protocoloRepository.count() + 1;
        return "PROT-%d-%04d".formatted(ano, sequencial);
    }

    private ProtocoloResponse toResponse(Protocolo protocolo) {
        Integer posicao = protocolo.getStatus() == StatusProtocolo.AGUARDANDO
                ? filaPriorizacaoService.posicaoNaFila(protocolo.getId()).orElse(null)
                : null;
        return ProtocoloResponse.de(protocolo, posicao);
    }
}
