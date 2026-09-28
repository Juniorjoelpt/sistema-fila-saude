package br.com.filasaude.service;

import br.com.filasaude.domain.HorarioAgenda;
import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.UnidadeSaude;
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

    private ProtocoloService service() {
        return new ProtocoloService(protocoloRepository, pacienteRepository, procedimentoRepository,
                unidadeSaudeRepository, historicoStatusRepository, historicoPrioridadeRepository,
                usuarioRepository, horarioAgendaRepository, etapasPadraoFactory, filaPriorizacaoService,
                notificacaoEmailService, auditoriaService);
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

        verify(horarioAgendaRepository, never()).findById(any());
    }

    @Test
    void agendarHorarioFalhaQuandoHorarioNaoExiste() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));
        when(horarioAgendaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().agendarHorario(10L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void agendarHorarioFalhaQuandoEspecialidadeNaoCorresponde() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));
        HorarioAgenda horario = horarioAgenda(99L, unidadeA, "Dermatologia",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), 5);
        when(horarioAgendaRepository.findById(99L)).thenReturn(Optional.of(horario));

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
        when(horarioAgendaRepository.findById(99L)).thenReturn(Optional.of(horario));

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
        when(horarioAgendaRepository.findById(99L)).thenReturn(Optional.of(horario));
        when(protocoloRepository.countByHorarioAgendadoIdAndStatusNot(99L, StatusProtocolo.CANCELADO)).thenReturn(2L);

        assertThatThrownBy(() -> service().agendarHorario(10L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Não há mais vagas");
    }

    @Test
    void agendarHorarioComSucessoAtualizaProtocoloENotifica() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));
        LocalDate data = LocalDate.now().plusDays(1);
        HorarioAgenda horario = horarioAgenda(99L, unidadeA, "Cardiologia", data, LocalTime.of(9, 0), 5);
        when(horarioAgendaRepository.findById(99L)).thenReturn(Optional.of(horario));
        when(protocoloRepository.countByHorarioAgendadoIdAndStatusNot(99L, StatusProtocolo.CANCELADO)).thenReturn(1L);
        when(protocoloRepository.save(any(Protocolo.class))).thenAnswer(inv -> inv.getArgument(0));

        service().agendarHorario(10L, 99L);

        assertThat(protocolo.getStatus()).isEqualTo(StatusProtocolo.AGENDADO);
        assertThat(protocolo.getUnidadeSaude()).isEqualTo(unidadeA);
        assertThat(protocolo.getHorarioAgendado()).isEqualTo(horario);
        assertThat(protocolo.getDataPrevista()).isEqualTo(data);
        verify(notificacaoEmailService).notificarMudancaStatus(protocolo);
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
        verify(notificacaoEmailService, never()).notificarMudancaStatus(any());
    }
}
