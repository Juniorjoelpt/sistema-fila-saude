package br.com.filasaude.service;

import br.com.filasaude.domain.HistoricoStatus;
import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.UnidadeSaude;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.domain.enums.CategoriaPrioridade;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.common.PageResponse;
import br.com.filasaude.dto.protocolo.DistribuirVagasRequest;
import br.com.filasaude.dto.protocolo.EtapaAdminResponse;
import br.com.filasaude.dto.protocolo.HistoricoStatusResponse;
import br.com.filasaude.dto.protocolo.ProtocoloCreateRequest;
import br.com.filasaude.dto.protocolo.ProtocoloDetalheResponse;
import br.com.filasaude.dto.protocolo.ProtocoloResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.HistoricoStatusRepository;
import br.com.filasaude.repository.PacienteRepository;
import br.com.filasaude.repository.ProcedimentoRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.repository.UnidadeSaudeRepository;
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
    private final EtapasPadraoFactory etapasPadraoFactory;
    private final FilaPriorizacaoService filaPriorizacaoService;
    private final NotificacaoEmailService notificacaoEmailService;

    public ProtocoloService(ProtocoloRepository protocoloRepository,
                             PacienteRepository pacienteRepository,
                             ProcedimentoRepository procedimentoRepository,
                             UnidadeSaudeRepository unidadeSaudeRepository,
                             HistoricoStatusRepository historicoStatusRepository,
                             EtapasPadraoFactory etapasPadraoFactory,
                             FilaPriorizacaoService filaPriorizacaoService,
                             NotificacaoEmailService notificacaoEmailService) {
        this.protocoloRepository = protocoloRepository;
        this.pacienteRepository = pacienteRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.unidadeSaudeRepository = unidadeSaudeRepository;
        this.historicoStatusRepository = historicoStatusRepository;
        this.etapasPadraoFactory = etapasPadraoFactory;
        this.filaPriorizacaoService = filaPriorizacaoService;
        this.notificacaoEmailService = notificacaoEmailService;
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

        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProtocoloResponse> listarFila(CategoriaPrioridade categoria, StatusProtocolo status,
                                                        Long procedimentoId, Long acsResponsavelId,
                                                        int page, int size) {
        Specification<Protocolo> spec = Specification
                .where(ProtocoloSpecifications.comCategoria(categoria))
                .and(ProtocoloSpecifications.comStatus(status))
                .and(ProtocoloSpecifications.comProcedimento(procedimentoId))
                .and(ProtocoloSpecifications.comAcsResponsavel(acsResponsavelId));

        List<Protocolo> todos = protocoloRepository.findAll(spec);

        Comparator<Protocolo> ordenacao = Comparator
                .<Protocolo>comparingInt(p -> p.getCategoriaPrioridade().getPeso())
                .thenComparing(Protocolo::getDataInclusao);
        List<Protocolo> ordenados = todos.stream().sorted(ordenacao).toList();

        int inicio = Math.min(page * size, ordenados.size());
        int fim = Math.min(inicio + size, ordenados.size());
        List<ProtocoloResponse> pagina = ordenados.subList(inicio, fim).stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.de(pagina, page, size, ordenados.size());
    }

    @Transactional(readOnly = true)
    public ProtocoloDetalheResponse buscarDetalhe(Long id) {
        Protocolo protocolo = buscarOuFalhar(id);
        ProtocoloResponse resumo = toResponse(protocolo);

        List<EtapaAdminResponse> etapas = protocolo.getEtapas().stream()
                .map(EtapaAdminResponse::de)
                .toList();

        List<HistoricoStatusResponse> historico = historicoStatusRepository
                .findByProtocoloIdOrderByCriadoEmAsc(id).stream()
                .map(HistoricoStatusResponse::de)
                .toList();

        return ProtocoloDetalheResponse.de(resumo, etapas, historico);
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
