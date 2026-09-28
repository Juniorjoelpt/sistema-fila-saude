package br.com.filasaude.service;

import br.com.filasaude.domain.HorarioAgenda;
import br.com.filasaude.domain.UnidadeSaude;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.agenda.HorarioAgendaCreateRequest;
import br.com.filasaude.dto.agenda.HorarioAgendaLoteRequest;
import br.com.filasaude.dto.agenda.HorarioAgendaResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.HorarioAgendaRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.repository.UnidadeSaudeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Optional;

import static br.com.filasaude.support.Fixtures.horarioAgenda;
import static br.com.filasaude.support.Fixtures.unidadeSaude;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Gestão da agenda de horários reais (unidade + especialidade + data + hora
 * + capacidade) -- cadastro individual, geração em lote (recorrente) e
 * remoção com proteção contra apagar horário já ocupado.
 */
@ExtendWith(MockitoExtension.class)
class HorarioAgendaServiceTest {

    @Mock
    private HorarioAgendaRepository horarioAgendaRepository;
    @Mock
    private UnidadeSaudeRepository unidadeSaudeRepository;
    @Mock
    private ProtocoloRepository protocoloRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private HorarioAgendaService service() {
        return new HorarioAgendaService(horarioAgendaRepository, unidadeSaudeRepository, protocoloRepository, auditoriaService);
    }

    private final UnidadeSaude unidade = unidadeSaude(1L, "UBS Central");

    @Test
    void naoPermiteCadastrarHorarioEmDataPassada() {
        lenient().when(unidadeSaudeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        HorarioAgendaCreateRequest request = new HorarioAgendaCreateRequest(
                1L, "Cardiologia", LocalDate.now().minusDays(1), LocalTime.of(9, 0), 5);

        assertThatThrownBy(() -> service().criar(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("data passada");
    }

    @Test
    void naoPermiteCadastrarHorarioDuplicado() {
        when(unidadeSaudeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        HorarioAgendaCreateRequest request = new HorarioAgendaCreateRequest(
                1L, "Cardiologia", LocalDate.now().plusDays(1), LocalTime.of(9, 0), 5);
        when(horarioAgendaRepository.findByUnidadeSaudeIdAndEspecialidadeAndDataAndHoraInicio(
                1L, "Cardiologia", request.data(), request.horaInicio()))
                .thenReturn(Optional.of(horarioAgenda(99L, unidade, "Cardiologia", request.data(), request.horaInicio(), 5)));

        assertThatThrownBy(() -> service().criar(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Já existe um horário");
    }

    @Test
    void criaHorarioComSucessoERegistraAuditoria() {
        when(unidadeSaudeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        HorarioAgendaCreateRequest request = new HorarioAgendaCreateRequest(
                1L, "Cardiologia", LocalDate.now().plusDays(1), LocalTime.of(9, 0), 5);
        when(horarioAgendaRepository.findByUnidadeSaudeIdAndEspecialidadeAndDataAndHoraInicio(
                1L, "Cardiologia", request.data(), request.horaInicio()))
                .thenReturn(Optional.empty());
        when(horarioAgendaRepository.save(any(HorarioAgenda.class)))
                .thenAnswer(inv -> {
                    HorarioAgenda h = inv.getArgument(0);
                    h.setId(10L);
                    return h;
                });
        when(protocoloRepository.countByHorarioAgendadoIdAndStatusNot(10L, StatusProtocolo.CANCELADO)).thenReturn(0L);

        HorarioAgendaResponse response = service().criar(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.vagasOcupadas()).isEqualTo(0L);
        verify(auditoriaService).registrar(eq("CRIAR_HORARIO_AGENDA"), eq("HorarioAgenda"), eq(10L), anyString());
    }

    @Test
    void geracaoEmLoteRejeitaDataFimAnteriorADataInicio() {
        when(unidadeSaudeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        HorarioAgendaLoteRequest request = new HorarioAgendaLoteRequest(
                1L, "Cardiologia", LocalDate.now().plusDays(5), LocalDate.now().plusDays(1),
                EnumSet.of(DayOfWeek.MONDAY), LocalTime.of(8, 0), LocalTime.of(12, 0), 20, 3);

        assertThatThrownBy(() -> service().gerarLote(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("data final");

        verify(horarioAgendaRepository, never()).save(any());
    }

    @Test
    void geracaoEmLoteRejeitaHoraFimNaoPosteriorAHoraInicio() {
        when(unidadeSaudeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        HorarioAgendaLoteRequest request = new HorarioAgendaLoteRequest(
                1L, "Cardiologia", LocalDate.now().plusDays(1), LocalDate.now().plusDays(1),
                EnumSet.of(DayOfWeek.MONDAY), LocalTime.of(12, 0), LocalTime.of(12, 0), 20, 3);

        assertThatThrownBy(() -> service().gerarLote(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hora final");
    }

    @Test
    void geracaoEmLotePulaHorariosJaExistentesSemDuplicar() {
        when(unidadeSaudeRepository.findById(1L)).thenReturn(Optional.of(unidade));

        // Uma única segunda-feira no período, 8h-9h de 30 em 30 min -> 2 horários possíveis (8h e 8h30).
        LocalDate segunda = proximaSegunda();
        HorarioAgendaLoteRequest request = new HorarioAgendaLoteRequest(
                1L, "Cardiologia", segunda, segunda,
                EnumSet.of(DayOfWeek.MONDAY), LocalTime.of(8, 0), LocalTime.of(9, 0), 30, 3);

        // 8h já existe -- deve ser pulado; 8h30 ainda não -- deve ser criado.
        when(horarioAgendaRepository.findByUnidadeSaudeIdAndEspecialidadeAndDataAndHoraInicio(
                1L, "Cardiologia", segunda, LocalTime.of(8, 0)))
                .thenReturn(Optional.of(horarioAgenda(1L, unidade, "Cardiologia", segunda, LocalTime.of(8, 0), 3)));
        when(horarioAgendaRepository.findByUnidadeSaudeIdAndEspecialidadeAndDataAndHoraInicio(
                1L, "Cardiologia", segunda, LocalTime.of(8, 30)))
                .thenReturn(Optional.empty());

        int criados = service().gerarLote(request);

        assertThat(criados).isEqualTo(1);
        ArgumentCaptor<HorarioAgenda> captor = ArgumentCaptor.forClass(HorarioAgenda.class);
        verify(horarioAgendaRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getHoraInicio()).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    void removerFalhaQuandoHorarioNaoExiste() {
        when(horarioAgendaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().remover(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removerFalhaQuandoHaProtocoloOcupandoOHorario() {
        HorarioAgenda horario = horarioAgenda(1L, unidade, "Cardiologia", LocalDate.now().plusDays(1), LocalTime.of(9, 0), 5);
        when(horarioAgendaRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(protocoloRepository.countByHorarioAgendadoIdAndStatusNot(1L, StatusProtocolo.CANCELADO)).thenReturn(2L);

        assertThatThrownBy(() -> service().remover(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Não é possível remover");

        verify(horarioAgendaRepository, never()).delete(any());
    }

    @Test
    void removeComSucessoQuandoNaoHaOcupacao() {
        HorarioAgenda horario = horarioAgenda(1L, unidade, "Cardiologia", LocalDate.now().plusDays(1), LocalTime.of(9, 0), 5);
        when(horarioAgendaRepository.findById(1L)).thenReturn(Optional.of(horario));
        when(protocoloRepository.countByHorarioAgendadoIdAndStatusNot(1L, StatusProtocolo.CANCELADO)).thenReturn(0L);

        service().remover(1L);

        verify(horarioAgendaRepository).delete(horario);
        verify(auditoriaService).registrar(eq("REMOVER_HORARIO_AGENDA"), eq("HorarioAgenda"), eq(1L), anyString());
    }

    private LocalDate proximaSegunda() {
        LocalDate data = LocalDate.now().plusDays(1);
        while (data.getDayOfWeek() != DayOfWeek.MONDAY) {
            data = data.plusDays(1);
        }
        return data;
    }
}
