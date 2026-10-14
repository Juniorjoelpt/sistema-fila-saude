package br.com.filasaude.service;

import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.SlaConfiguracao;
import br.com.filasaude.dto.slaconfiguracao.SlaConfiguracaoRequest;
import br.com.filasaude.dto.slaconfiguracao.SlaConfiguracaoResponse;
import br.com.filasaude.dto.slaconfiguracao.SlaConfiguracoesResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.ProcedimentoRepository;
import br.com.filasaude.repository.SlaConfiguracaoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SLA configurável por especialidade/procedimento (roadmap "Ganhos
 * rápidos"): antes, o limite de dias para os badges de prazo "Atenção"/
 * "Atrasado" era um único valor fixo para o tenant inteiro. Esta classe
 * resolve, para cada protocolo, qual limite vale -- o override de
 * procedimento (mais específico), senão o de especialidade, senão o padrão
 * do tenant -- e é o ÚNICO lugar que decide isso: DashboardService,
 * SlaAlertaTenantProcessor e o indicador de prazo da fila (via
 * {@link #nivelPrazo(Protocolo)}) todos passam por aqui, para nunca
 * divergir sobre o que é "Atrasado".
 */
@Service
@Transactional(readOnly = true)
public class SlaConfiguracaoService {

    /** Resultado da resolução: os dois limiares (em dias) que valem para um protocolo. */
    public record LimiteSla(int diasAtencao, int diasAtrasado) {
    }

    private final SlaConfiguracaoRepository slaConfiguracaoRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final AuditoriaService auditoriaService;
    private final int diasAtencaoPadrao;
    private final int diasAtrasadoPadrao;

    public SlaConfiguracaoService(SlaConfiguracaoRepository slaConfiguracaoRepository,
                                   ProcedimentoRepository procedimentoRepository,
                                   AuditoriaService auditoriaService,
                                   @Value("${filasaude.sla.dias-limite-atencao:7}") int diasAtencaoPadrao,
                                   @Value("${filasaude.sla.dias-limite-atrasado:15}") int diasAtrasadoPadrao) {
        this.slaConfiguracaoRepository = slaConfiguracaoRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.auditoriaService = auditoriaService;
        this.diasAtencaoPadrao = diasAtencaoPadrao;
        this.diasAtrasadoPadrao = diasAtrasadoPadrao;
    }

    public int diasAtrasadoPadrao() {
        return diasAtrasadoPadrao;
    }

    public SlaConfiguracoesResponse listar() {
        List<SlaConfiguracaoResponse> configuracoes = slaConfiguracaoRepository
                .findAllByOrderByEspecialidadeAscProcedimentoIdAsc().stream()
                .map(SlaConfiguracaoResponse::de)
                .toList();
        return new SlaConfiguracoesResponse(diasAtencaoPadrao, diasAtrasadoPadrao, configuracoes);
    }

    @Transactional
    public SlaConfiguracaoResponse criar(SlaConfiguracaoRequest request) {
        validarEscopoEDias(request);

        SlaConfiguracao.SlaConfiguracaoBuilder builder = SlaConfiguracao.builder()
                .diasAtencao(request.diasAtencao())
                .diasAtrasado(request.diasAtrasado());

        String descricaoEscopo;
        if (request.procedimentoId() != null) {
            Procedimento procedimento = procedimentoRepository.findById(request.procedimentoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Procedimento não encontrado: " + request.procedimentoId()));
            if (slaConfiguracaoRepository.findByProcedimentoId(request.procedimentoId()).isPresent()) {
                throw new IllegalStateException(
                        "Já existe uma configuração de SLA para este procedimento");
            }
            builder.procedimento(procedimento);
            descricaoEscopo = "procedimento '" + procedimento.getNome() + "'";
        } else {
            if (slaConfiguracaoRepository.findByEspecialidade(request.especialidade()).isPresent()) {
                throw new IllegalStateException(
                        "Já existe uma configuração de SLA para esta especialidade");
            }
            builder.especialidade(request.especialidade());
            descricaoEscopo = "especialidade '" + request.especialidade() + "'";
        }

        SlaConfiguracao salvo = slaConfiguracaoRepository.save(builder.build());

        auditoriaService.registrar("CRIAR_SLA_CONFIGURACAO", "SlaConfiguracao", salvo.getId(),
                "SLA de " + descricaoEscopo + ": atenção " + salvo.getDiasAtencao()
                        + " dias, atrasado " + salvo.getDiasAtrasado() + " dias");

        return SlaConfiguracaoResponse.de(salvo);
    }

    @Transactional
    public SlaConfiguracaoResponse atualizar(Long id, SlaConfiguracaoRequest request) {
        SlaConfiguracao config = slaConfiguracaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuração de SLA não encontrada: " + id));

        validarEscopoEDias(request);

        // O escopo (especialidade/procedimento) de uma configuração já criada não
        // é editável aqui -- trocar o escopo é, na prática, criar outra
        // configuração; editar só os limiares evita o risco de uma edição
        // "silenciosa" mudar qual protocolo o override afeta.
        boolean escopoProcedimentoBate = request.procedimentoId() != null
                && config.getProcedimento() != null
                && request.procedimentoId().equals(config.getProcedimento().getId());
        boolean escopoEspecialidadeBate = request.especialidade() != null
                && request.especialidade().equals(config.getEspecialidade());
        if (!escopoProcedimentoBate && !escopoEspecialidadeBate) {
            throw new IllegalStateException(
                    "O escopo (especialidade ou procedimento) de uma configuração de SLA não pode ser alterado; exclua e crie uma nova");
        }

        int anteriorAtencao = config.getDiasAtencao();
        int anteriorAtrasado = config.getDiasAtrasado();

        config.setDiasAtencao(request.diasAtencao());
        config.setDiasAtrasado(request.diasAtrasado());
        config.setAtualizadoEm(LocalDateTime.now());
        slaConfiguracaoRepository.save(config);

        auditoriaService.registrar("ATUALIZAR_SLA_CONFIGURACAO", "SlaConfiguracao", config.getId(),
                "Limiares alterados de atenção " + anteriorAtencao + "/atrasado " + anteriorAtrasado
                        + " para atenção " + config.getDiasAtencao() + "/atrasado " + config.getDiasAtrasado());

        return SlaConfiguracaoResponse.de(config);
    }

    @Transactional
    public void excluir(Long id) {
        SlaConfiguracao config = slaConfiguracaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuração de SLA não encontrada: " + id));
        slaConfiguracaoRepository.deleteById(id);
        auditoriaService.registrar("EXCLUIR_SLA_CONFIGURACAO", "SlaConfiguracao", id,
                "Configuração de SLA removida (escopo: "
                        + (config.getProcedimento() != null ? "procedimento '" + config.getProcedimento().getNome() + "'"
                                : "especialidade '" + config.getEspecialidade() + "'") + ")");
    }

    /**
     * Resolve os limiares que valem para um protocolo: override de
     * procedimento (mais específico) > override de especialidade > padrão
     * do tenant. Carrega a lista inteira em memória (poucas dezenas de
     * linhas no máximo) em vez de duas queries por protocolo, já que os
     * chamadores (Dashboard, varredura de alerta) sempre resolvem vários
     * protocolos de uma vez.
     */
    public LimiteSla resolverLimites(Protocolo protocolo, List<SlaConfiguracao> configuracoes) {
        Long procedimentoId = protocolo.getProcedimento() != null ? protocolo.getProcedimento().getId() : null;
        String especialidade = protocolo.getProcedimento() != null ? protocolo.getProcedimento().getEspecialidade() : null;

        Optional<SlaConfiguracao> porProcedimento = procedimentoId == null ? Optional.empty()
                : configuracoes.stream()
                        .filter(c -> c.getProcedimento() != null && c.getProcedimento().getId().equals(procedimentoId))
                        .findFirst();
        if (porProcedimento.isPresent()) {
            SlaConfiguracao c = porProcedimento.get();
            return new LimiteSla(c.getDiasAtencao(), c.getDiasAtrasado());
        }

        Optional<SlaConfiguracao> porEspecialidade = especialidade == null ? Optional.empty()
                : configuracoes.stream()
                        .filter(c -> especialidade.equals(c.getEspecialidade()))
                        .findFirst();
        if (porEspecialidade.isPresent()) {
            SlaConfiguracao c = porEspecialidade.get();
            return new LimiteSla(c.getDiasAtencao(), c.getDiasAtrasado());
        }

        return new LimiteSla(diasAtencaoPadrao, diasAtrasadoPadrao);
    }

    /** Carrega todas as configurações uma vez -- ver {@link #resolverLimites}. */
    public List<SlaConfiguracao> carregarConfiguracoes() {
        return slaConfiguracaoRepository.findAll();
    }

    /** {@code true} quando o protocolo já ultrapassou o limiar "Atrasado" resolvido para ele. */
    public boolean estaAtrasado(Protocolo protocolo, List<SlaConfiguracao> configuracoes) {
        return protocolo.diasEmEspera() > resolverLimites(protocolo, configuracoes).diasAtrasado();
    }

    /** {@code true} na faixa "Atenção": passou do limiar de atenção, mas ainda não do de "Atrasado". */
    public boolean estaEmAtencao(Protocolo protocolo, List<SlaConfiguracao> configuracoes) {
        LimiteSla limite = resolverLimites(protocolo, configuracoes);
        long dias = protocolo.diasEmEspera();
        return dias > limite.diasAtencao() && dias <= limite.diasAtrasado();
    }

    private void validarEscopoEDias(SlaConfiguracaoRequest request) {
        boolean temEspecialidade = request.especialidade() != null && !request.especialidade().isBlank();
        boolean temProcedimento = request.procedimentoId() != null;
        if (temEspecialidade == temProcedimento) {
            throw new IllegalArgumentException(
                    "Informe exatamente um dos dois: especialidade OU procedimento");
        }
        if (request.diasAtrasado() <= request.diasAtencao()) {
            throw new IllegalArgumentException(
                    "O limiar de dias 'Atrasado' deve ser maior que o de 'Atenção'");
        }
    }
}
