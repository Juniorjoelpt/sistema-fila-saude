package br.com.filasaude.domain;

import br.com.filasaude.domain.enums.CategoriaPrioridade;
import br.com.filasaude.domain.enums.PresencaConfirmacao;
import br.com.filasaude.domain.enums.StatusProtocolo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um item da fila de regulacao: o vinculo entre um paciente e um
 * procedimento, com sua prioridade, status e trilha de etapas (timeline).
 * E o "protocolo" que o cidadao consulta publicamente por CPF/CNS (item 3.1).
 */
@Entity
@Table(name = "protocolos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Protocolo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_protocolo", nullable = false, unique = true, length = 30)
    private String numeroProtocolo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedimento_id")
    private Procedimento procedimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_saude_id")
    private UnidadeSaude unidadeSaude;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_prioridade", nullable = false, length = 20)
    @Builder.Default
    private CategoriaPrioridade categoriaPrioridade = CategoriaPrioridade.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusProtocolo status = StatusProtocolo.AGUARDANDO;

    /** Numero do processo/mandado judicial, quando categoria = JUDICIAL (item 3.2). */
    @Column(name = "processo_judicial", length = 60)
    private String processoJudicial;

    @Column(name = "data_solicitacao", nullable = false)
    private LocalDate dataSolicitacao;

    @Column(name = "data_inclusao", nullable = false)
    @Builder.Default
    private LocalDate dataInclusao = LocalDate.now();

    @Column(name = "data_prevista")
    private LocalDate dataPrevista;

    /**
     * Horario real (unidade + especialidade + data + hora, com controle de
     * vaga) para o qual este protocolo foi agendado, quando o agendamento
     * foi feito pela tela de "Agendar horário" (ver
     * ProtocoloService.agendarHorario). NULL para protocolos que nunca
     * passaram por ali (ex.: ainda aguardando, ou agendados pelo fluxo
     * antigo "Distribuir Vagas", que so seta dataPrevista sem horario).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horario_agenda_id")
    private HorarioAgenda horarioAgendado;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    /**
     * Quando o alerta de SLA vencido (prazo "Atrasado") foi enviado para os
     * reguladores/admins do tenant. NULL = ainda não alertado. Evita reenvio
     * duplicado na varredura diária do {@code SlaAlertaService}.
     */
    @Column(name = "alerta_sla_enviado_em")
    private LocalDateTime alertaSlaEnviadoEm;

    /**
     * Lembrete de agendamento + confirmação de presença (melhoria pós-MVP
     * sobre o agendamento de horário real -- ver {@code HorarioAgenda}):
     * token opaco enviado por e-mail um dia antes do horário marcado, que dá
     * ao paciente um link público para confirmar ou cancelar a presença sem
     * precisar logar nem informar CPF/CNS de novo. NULL até que um lembrete
     * tenha sido enviado (ver {@code LembreteAgendamentoService}).
     */
    @Column(name = "confirmacao_token", unique = true, length = 36)
    private String confirmacaoToken;

    @Column(name = "lembrete_enviado_em")
    private LocalDateTime lembreteEnviadoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "presenca_confirmacao", length = 20)
    private PresencaConfirmacao presencaConfirmacao;

    @OneToMany(mappedBy = "protocolo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    @Builder.Default
    private List<EtapaProtocolo> etapas = new ArrayList<>();

    public long diasEmEspera() {
        return java.time.temporal.ChronoUnit.DAYS.between(dataInclusao, LocalDate.now());
    }
}
