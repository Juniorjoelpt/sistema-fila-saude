package br.com.filasaude.service;

import br.com.filasaude.domain.EtapaProtocolo;
import br.com.filasaude.domain.HorarioAgenda;
import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.UnidadeSaude;
import br.com.filasaude.domain.enums.StatusEtapa;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.HistoricoPrioridadeRepository;
import br.com.filasaude.repository.HistoricoStatusRepository;
import br.com.filasaude.repository.HorarioAgendaRepository;
import br.com.filasaude.repository.PacienteRepository;
import br.com.filasaude.repository.ProcedimentoRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.repository.UnidadeSaudeRepository;
import br.com.filasaude.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static br.com.filasaude.support.Fixtures.horarioAgenda;
import static br.com.filasaude.support.Fixtures.paciente;
import static br.com.filasaude.support.Fixtures.procedimento;
import static br.com.filasaude.support.Fixtures.protocolo;
import static br.com.filasaude.support.Fixtures.unidadeSaude;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Agendamento em horário real (unidade + especialidade + data + hora, com
 * controle de vaga) e a máquina de estados de status -- em especial a
 * regressão de "desagendar" (AGENDADO -> AGUARDANDO) precisar soltar o
 * horário ocupado, bug encontrado via teste manual no Chrome mais cedo
 * nesta mesma iniciativa.
 */
@ExtendWith(MockitoExtension.class)
class ProtocoloServiceAgendamentoTest {

    @Mock private ProtocoloRepository protocoloRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private ProcedimentoRepository procedimentoRepository;
    @Mock private UnidadeSaudeRepository unidadeSaudeRepository;
    @Mock private HistoricoStatusRepository historicoStatusRepository;
    @Mock private HistoricoPrioridadeRepository historicoPrioridadeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HorarioAgendaRepository horarioAgendaRepository;
    @Mock private EtapasPadraoFactory etapasPadraoFactory;
    @Mock private FilaPriorizacaoService filaPriorizacaoService;
    @Mock private NotificacaoEmailService notificacaoEmailService;
    @Mock private AuditoriaService auditoriaService;
    @Mock private CotaService cotaService;
    private final ClassificacaoRiscoService classificacaoRiscoService = new ClassificacaoRiscoService();

    private ProtocoloService service() {
        return new ProtocoloService(protocoloRepository, pacienteRepository, procedimentoRepository,
                unidadeSaudeRepository, historicoStatusRepository, historicoPrioridadeRepository,
                usuarioRepository, horarioAgendaRepository, etapasPadraoFactory, filaPriorizacaoService,
                notificacaoEmailService, auditoriaService, cotaService, classificacaoRiscoService);
    }

    private final Paciente paciente = paciente(1L, "Paciente Teste");
    private final Procedimento procedimento = procedimento(1L, "Consulta Cardiologia", "Cardiologia");
    private final UnidadeSaude unidadeA = unidadeSaude(1L, "UBS Central");
    private final UnidadeSaude unidadeB = unidadeSaude(2L, "UBS Norte");

    @Test
    void agendarHorarioFalhaQuandoTransicaoNaoPermitida() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setStatus(StatusProtocolo.CONCLUIDO);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));

        assertThatThrownBy(() -> service().agendarHorario(10L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transição de status não permitida");

        verify(horarioAgendaRepository, never()).buscarComLockParaAgendamento(any());
    }

    @Test
    void agendarHorarioFalhaQuandoHorarioNaoExiste() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));
        when(horarioAgendaRepository.buscarComLockParaAgendamento(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().agendarHorario(10L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void agendarHorarioFalhaQuandoEspecialidadeNaoCorresponde() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));
        HorarioAgenda horario = horarioAgenda(99L, unidadeA, "Dermatologia",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), 5);
        when(horarioAgendaRepository.buscarComLockParaAgendamento(99L)).thenReturn(Optional.of(horario));

        assertThatThrownBy(() -> service().agendarHorario(10L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("especialidade");
    }

    @Test
    void agendarHorarioFalhaQuandoUnidadeNaoCorresponde() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setUnidadeSaude(unidadeA);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));
        HorarioAgenda horario = horarioAgenda(99L, unidadeB, "Cardiologia",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), 5);
        when(horarioAgendaRepository.buscarComLockParaAgendamento(99L)).thenReturn(Optional.of(horario));

        assertThatThrownBy(() -> service().agendarHorario(10L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outra unidade de saúde");
    }

    @Test
    void agendarHorarioFalhaQuandoNaoHaVaga() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));
        HorarioAgenda horario = horarioAgenda(99L, unidadeA, "Cardiologia",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), 2);
        when(horarioAgendaRepository.buscarComLockParaAgendamento(99L)).thenReturn(Optional.of(horario));
        when(protocoloRepository.countByHorarioAgendadoIdAndStatusNot(99L, StatusProtocolo.CANCELADO)).thenReturn(2L);

        assertThatThrownBy(() -> service().agendarHorario(10L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Não há mais vagas");
    }

    @Test
    void agendarHorarioFalhaQuandoCotaEsgotada() {
        // Regressão do bug alto de revisão: cota nunca era verificada no
        // momento do agendamento (ver Javadoc de CotaService#temVagaDisponivel).
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));
        LocalDate data = LocalDate.now().plusDays(1);
        HorarioAgenda horario = horarioAgenda(99L, unidadeA, "Cardiologia", data, LocalTime.of(9, 0), 5);
        when(horarioAgendaRepository.buscarComLockParaAgendamento(99L)).thenReturn(Optional.of(horario));
        when(protocoloRepository.countByHorarioAgendadoIdAndStatusNot(99L, StatusProtocolo.CANCELADO)).thenReturn(1L);
        when(cotaService.temVagaDisponivel(unidadeA.getId(), "Cardiologia", data)).thenReturn(false);

        assertThatThrownBy(() -> service().agendarHorario(10L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cota esgotada");

        verify(protocoloRepository, never()).save(any());
    }

    @Test
    void agendarHorarioComSucessoAtualizaProtocoloENotifica() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));
        LocalDate data = LocalDate.now().plusDays(1);
        HorarioAgenda horario = horarioAgenda(99L, unidadeA, "Cardiologia", data, LocalTime.of(9, 0), 5);
        when(horarioAgendaRepository.buscarComLockParaAgendamento(99L)).thenReturn(Optional.of(horario));
        when(protocoloRepository.countByHorarioAgendadoIdAndStatusNot(99L, StatusProtocolo.CANCELADO)).thenReturn(1L);
        when(protocoloRepository.save(any(Protocolo.class))).thenAnswer(inv -> inv.getArgument(0));
        // Sem cota cadastrada para esta unidade/especialidade/mês -- CotaService#temVagaDisponivel
        // retorna true nesse caso (ver Javadoc do método real); aqui simulamos o mesmo.
        when(cotaService.temVagaDisponivel(eq(unidadeA.getId()), eq("Cardiologia"), eq(data))).thenReturn(true);

        service().agendarHorario(10L, 99L);

        assertThat(protocolo.getStatus()).isEqualTo(StatusProtocolo.AGENDADO);
        assertThat(protocolo.getUnidadeSaude()).isEqualTo(unidadeA);
        assertThat(protocolo.getHorarioAgendado()).isEqualTo(horario);
        assertThat(protocolo.getDataPrevista()).isEqualTo(data);
        verify(notificacaoEmailService).notificarMudancaStatus(any(NotificacaoEmailService.NotificacaoStatus.class));
        verify(auditoriaService).registrar(eq("AGENDAR_HORARIO_PROTOCOLO"), eq("Protocolo"), eq(10L), anyString());
    }

    /**
     * Regressão: bug encontrado em teste manual no Chrome -- ao "desagendar"
     * (AGENDADO -> AGUARDANDO), o horário real vinculado ficava preso ao
     * protocolo, mostrando a vaga como ocupada para sempre mesmo sem
     * ninguém mais agendado ali.
     */
    @Test
    void mudarStatusDeAgendadoParaAguardandoLimpaHorarioEDataPrevista() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setStatus(StatusProtocolo.AGENDADO);
        HorarioAgenda horario = horarioAgenda(99L, unidadeA, "Cardiologia",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), 5);
        protocolo.setHorarioAgendado(horario);
        protocolo.setDataPrevista(LocalDate.now().plusDays(1));
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));
        when(protocoloRepository.save(any(Protocolo.class))).thenAnswer(inv -> inv.getArgument(0));

        service().mudarStatus(10L, StatusProtocolo.AGUARDANDO, "Paciente pediu para remarcar");

        assertThat(protocolo.getStatus()).isEqualTo(StatusProtocolo.AGUARDANDO);
        assertThat(protocolo.getHorarioAgendado()).isNull();
        assertThat(protocolo.getDataPrevista()).isNull();
    }

    @Test
    void mudarStatusRejeitaTransicaoDeEstadoTerminal() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setStatus(StatusProtocolo.CANCELADO);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));

        assertThatThrownBy(() -> service().mudarStatus(10L, StatusProtocolo.AGENDADO, null))
                .isInstanceOf(IllegalStateException.class);

        verify(protocoloRepository, never()).save(any());
        verify(notificacaoEmailService, never()).notificarMudancaStatus(any(NotificacaoEmailService.NotificacaoStatus.class));
    }

    // ---- Correções do teste prático (A3, A4, M2, M4) ----

    private EtapaProtocolo etapa(Long id, String nome, int ordem, StatusEtapa status) {
        return EtapaProtocolo.builder().id(id).nomeEtapa(nome).ordem(ordem).status(status).build();
    }

    @Test
    void mudarStatusParaOMesmoStatusNaoGravaNada() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));

        service().mudarStatus(10L, StatusProtocolo.AGUARDANDO, null);

        verify(protocoloRepository, never()).save(any());
        verify(historicoStatusRepository, never()).save(any());
        verify(notificacaoEmailService, never()).notificarMudancaStatus(any(NotificacaoEmailService.NotificacaoStatus.class));
    }

    @Test
    void mudarStatusNaoPermiteDefinirAgendadoDiretamente() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));

        assertThatThrownBy(() -> service().mudarStatus(10L, StatusProtocolo.AGENDADO, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Agendar horário");

        verify(protocoloRepository, never()).save(any());
    }

    @Test
    void cancelarExigeMotivo() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));

        assertThatThrownBy(() -> service().mudarStatus(10L, StatusProtocolo.CANCELADO, "  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void concluirProtocoloFechaTodasAsEtapas() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setStatus(StatusProtocolo.EM_ANDAMENTO);
        EtapaProtocolo jaRealizada = etapa(1L, "A", 1, StatusEtapa.REALIZADO);
        jaRealizada.setDataRealizacao(LocalDate.now().minusDays(3));
        protocolo.setEtapas(new ArrayList<>(List.of(
                jaRealizada,
                etapa(2L, "B", 2, StatusEtapa.EM_ANDAMENTO),
                etapa(3L, "C", 3, StatusEtapa.AGUARDANDO))));
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));
        when(protocoloRepository.save(any(Protocolo.class))).thenAnswer(inv -> inv.getArgument(0));

        service().mudarStatus(10L, StatusProtocolo.CONCLUIDO, null);

        assertThat(protocolo.getEtapas()).allMatch(e -> e.getStatus() == StatusEtapa.REALIZADO);
        assertThat(protocolo.getEtapas()).allMatch(e -> e.getDataRealizacao() != null);
    }

    @Test
    void marcarEtapaRecusaSegundaEtapaEmAndamento() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setEtapas(new ArrayList<>(List.of(
                etapa(1L, "A", 1, StatusEtapa.EM_ANDAMENTO),
                etapa(2L, "B", 2, StatusEtapa.AGUARDANDO))));
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));

        assertThatThrownBy(() -> service().marcarEtapa(10L, 2L, StatusEtapa.EM_ANDAMENTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já está em andamento");
    }

    @Test
    void marcarEtapaRecusaPularEtapaAnterior() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setEtapas(new ArrayList<>(List.of(
                etapa(1L, "A", 1, StatusEtapa.AGUARDANDO),
                etapa(2L, "B", 2, StatusEtapa.AGUARDANDO),
                etapa(3L, "C", 3, StatusEtapa.AGUARDANDO))));
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));

        assertThatThrownBy(() -> service().marcarEtapa(10L, 3L, StatusEtapa.REALIZADO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conclua primeiro a etapa \"A\"");
    }

    @Test
    void marcarEtapaRecusaReabrirEtapaComPosteriorIniciada() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        EtapaProtocolo a = etapa(1L, "A", 1, StatusEtapa.REALIZADO);
        a.setDataRealizacao(LocalDate.now().minusDays(2));
        protocolo.setEtapas(new ArrayList<>(List.of(a, etapa(2L, "B", 2, StatusEtapa.EM_ANDAMENTO))));
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));

        assertThatThrownBy(() -> service().marcarEtapa(10L, 1L, StatusEtapa.AGUARDANDO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já foi iniciada");
    }

    @Test
    void marcarEtapaLimpaDataAoReabrir() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        EtapaProtocolo realizada = etapa(1L, "A", 1, StatusEtapa.REALIZADO);
        realizada.setDataRealizacao(LocalDate.now().minusDays(2));
        protocolo.setEtapas(new ArrayList<>(List.of(realizada)));
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));
        when(protocoloRepository.save(any(Protocolo.class))).thenAnswer(inv -> inv.getArgument(0));

        service().marcarEtapa(10L, 1L, StatusEtapa.EM_ANDAMENTO);

        assertThat(realizada.getStatus()).isEqualTo(StatusEtapa.EM_ANDAMENTO);
        assertThat(realizada.getDataRealizacao()).isNull();
    }

    @Test
    void marcarEtapaRecusaProtocoloEncerrado() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setStatus(StatusProtocolo.CONCLUIDO);
        protocolo.setEtapas(new ArrayList<>(List.of(etapa(1L, "A", 1, StatusEtapa.REALIZADO))));
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));

        assertThatThrownBy(() -> service().marcarEtapa(10L, 1L, StatusEtapa.AGUARDANDO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("concluído");
    }

    @Test
    void diasEmEsperaParaDeContarQuandoProtocoloEstaConcluido() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setDataInclusao(LocalDate.now().minusDays(30));
        protocolo.setStatus(StatusProtocolo.CONCLUIDO);
        protocolo.setAtualizadoEm(java.time.LocalDateTime.now().minusDays(20));

        assertThat(protocolo.diasEmEspera()).isEqualTo(10);
    }
}
