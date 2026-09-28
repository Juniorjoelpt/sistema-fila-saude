package br.com.filasaude.service;

import br.com.filasaude.domain.Cota;
import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.UnidadeSaude;
import br.com.filasaude.domain.enums.PresencaConfirmacao;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.dashboard.DashboardResponse;
import br.com.filasaude.repository.CotaRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static br.com.filasaude.support.Fixtures.paciente;
import static br.com.filasaude.support.Fixtures.procedimento;
import static br.com.filasaude.support.Fixtures.protocolo;
import static br.com.filasaude.support.Fixtures.unidadeSaude;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Painel gerencial (item 3.4): fila, ocupação de cotas, SLA, tempo médio de
 * espera por especialidade e taxa de confirmação de presença.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final int DIAS_LIMITE_ATRASADO = 15;

    @Mock private ProtocoloRepository protocoloRepository;
    @Mock private CotaRepository cotaRepository;

    private DashboardService service() {
        return new DashboardService(protocoloRepository, cotaRepository, DIAS_LIMITE_ATRASADO);
    }

    private final Paciente paciente = paciente(1L, "Paciente Teste");
    private final UnidadeSaude unidade = unidadeSaude(1L, "UBS Central");

    private Protocolo protocoloAguardando(Long id, String especialidade, LocalDate dataInclusao) {
        Procedimento procedimento = procedimento(id, "Consulta " + especialidade, especialidade);
        Protocolo p = protocolo(id, paciente, procedimento);
        p.setStatus(StatusProtocolo.AGUARDANDO);
        p.setDataInclusao(dataInclusao);
        return p;
    }

    @Test
    void semNinguemAguardandoSlaEOcupacaoFicamNulos() {
        when(protocoloRepository.countByStatus(StatusProtocolo.AGUARDANDO)).thenReturn(0L);
        when(protocoloRepository.findByStatus(StatusProtocolo.AGUARDANDO)).thenReturn(List.of());
        when(protocoloRepository.findByStatus(StatusProtocolo.AGENDADO)).thenReturn(List.of());
        when(protocoloRepository.findByStatus(StatusProtocolo.CONCLUIDO)).thenReturn(List.of());
        when(protocoloRepository.countByStatusAndDataInclusaoLessThanEqual(any(), any())).thenReturn(0L);
        when(protocoloRepository.countByPresencaConfirmacao(any())).thenReturn(0L);
        when(cotaRepository.findByMesReferenciaOrderByUnidadeSaude_NomeAscEspecialidadeAsc(any())).thenReturn(List.of());
        when(cotaRepository.findAll()).thenReturn(List.of());

        DashboardResponse response = service().gerar();

        assertThat(response.filaDeEspera()).isZero();
        assertThat(response.sla().percentualDentroPrazo()).isNull();
        assertThat(response.sla().totalAguardando()).isZero();
        assertThat(response.taxaOcupacaoGeral()).isNull();
        assertThat(response.picoOcupacaoGeral()).isNull();
        assertThat(response.confirmacaoPresenca().percentualConfirmacao()).isNull();
        assertThat(response.confirmacaoPresenca().totalLembretesEnviados()).isZero();
    }

    @Test
    void calculaSlaOcupacaoETempoMedioComDadosCompletos() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = YearMonth.from(hoje).atDay(1);
        LocalDate fimMes = YearMonth.from(hoje).atEndOfMonth();

        Protocolo atrasado = protocoloAguardando(1L, "Cardiologia", hoje.minusDays(20));
        Protocolo dentroDoPrazo = protocoloAguardando(2L, "Dermatologia", hoje.minusDays(5));
        List<Protocolo> aguardando = List.of(atrasado, dentroDoPrazo);

        when(protocoloRepository.countByStatus(StatusProtocolo.AGUARDANDO)).thenReturn(2L);
        when(protocoloRepository.findByStatus(StatusProtocolo.AGUARDANDO)).thenReturn(aguardando);
        when(protocoloRepository.countByStatusAndDataInclusaoLessThanEqual(
                StatusProtocolo.AGUARDANDO, hoje.minusDays(DIAS_LIMITE_ATRASADO))).thenReturn(1L);

        // Agendados: um no mês corrente, outro fora -- só o primeiro deve contar.
        Procedimento procedimentoAgendado = procedimento(3L, "Consulta Cardiologia", "Cardiologia");
        Protocolo agendadoNoMes = protocolo(3L, paciente, procedimentoAgendado);
        agendadoNoMes.setStatus(StatusProtocolo.AGENDADO);
        agendadoNoMes.setDataPrevista(hoje.plusDays(2));
        Protocolo agendadoForaDoMes = protocolo(4L, paciente, procedimentoAgendado);
        agendadoForaDoMes.setStatus(StatusProtocolo.AGENDADO);
        agendadoForaDoMes.setDataPrevista(hoje.plusMonths(3));
        when(protocoloRepository.findByStatus(StatusProtocolo.AGENDADO))
                .thenReturn(List.of(agendadoNoMes, agendadoForaDoMes));

        // Concluídos: um no ano corrente, outro em ano anterior -- só o primeiro conta em "realizadosNoAno".
        Procedimento procedimentoConcluido = procedimento(5L, "Consulta Cardiologia", "Cardiologia");
        Protocolo concluidoNoAno = protocolo(5L, paciente, procedimentoConcluido);
        concluidoNoAno.setStatus(StatusProtocolo.CONCLUIDO);
        concluidoNoAno.setDataInclusao(hoje.minusDays(30));
        concluidoNoAno.setAtualizadoEm(LocalDateTime.now().minusDays(10));
        Protocolo concluidoAnoAnterior = protocolo(6L, paciente, procedimentoConcluido);
        concluidoAnoAnterior.setStatus(StatusProtocolo.CONCLUIDO);
        concluidoAnoAnterior.setDataInclusao(hoje.minusYears(1).minusDays(30));
        concluidoAnoAnterior.setAtualizadoEm(LocalDateTime.now().minusYears(1));
        when(protocoloRepository.findByStatus(StatusProtocolo.CONCLUIDO))
                .thenReturn(List.of(concluidoNoAno, concluidoAnoAnterior));

        // Confirmação de presença: 3 confirmados, 1 cancelado, 2 pendentes.
        when(protocoloRepository.countByPresencaConfirmacao(PresencaConfirmacao.CONFIRMADA)).thenReturn(3L);
        when(protocoloRepository.countByPresencaConfirmacao(PresencaConfirmacao.CANCELADA)).thenReturn(1L);
        when(protocoloRepository.countByPresencaConfirmacao(PresencaConfirmacao.PENDENTE)).thenReturn(2L);

        // Ocupação de cotas: Cardiologia com cota de 10 vagas e 9 utilizadas -> 90%, gargalo.
        Cota cotaCardiologia = Cota.builder()
                .id(1L).unidadeSaude(unidade).especialidade("Cardiologia")
                .mesReferencia(inicioMes).quantidadeTotal(10).build();
        when(cotaRepository.findByMesReferenciaOrderByUnidadeSaude_NomeAscEspecialidadeAsc(inicioMes))
                .thenReturn(List.of(cotaCardiologia));
        when(cotaRepository.findAll()).thenReturn(List.of(cotaCardiologia));

        List<Protocolo> utilizadosCardiologia = List.of(
                protocolo(7L, paciente, procedimentoConcluido), protocolo(8L, paciente, procedimentoConcluido),
                protocolo(9L, paciente, procedimentoConcluido), protocolo(10L, paciente, procedimentoConcluido),
                protocolo(11L, paciente, procedimentoConcluido), protocolo(12L, paciente, procedimentoConcluido),
                protocolo(13L, paciente, procedimentoConcluido), protocolo(14L, paciente, procedimentoConcluido),
                protocolo(15L, paciente, procedimentoConcluido));
        lenient().when(protocoloRepository.findByUnidadeSaudeIdAndProcedimento_EspecialidadeAndDataInclusaoBetween(
                        1L, "Cardiologia", inicioMes, fimMes))
                .thenReturn(utilizadosCardiologia);

        DashboardResponse response = service().gerar();

        assertThat(response.filaDeEspera()).isEqualTo(2);
        assertThat(response.agendadosNoMes()).isEqualTo(1);
        assertThat(response.realizadosNoAno()).isEqualTo(1);

        assertThat(response.sla().totalAguardando()).isEqualTo(2);
        assertThat(response.sla().totalAtrasado()).isEqualTo(1);
        assertThat(response.sla().totalDentroPrazo()).isEqualTo(1);
        assertThat(response.sla().percentualDentroPrazo()).isEqualTo(50);

        assertThat(response.taxaOcupacaoGeral()).isEqualTo(90);
        assertThat(response.picoOcupacaoGeral()).isEqualTo(90);
        assertThat(response.ocupacaoPorEspecialidade()).hasSize(1);
        assertThat(response.ocupacaoPorEspecialidade().get(0).gargalo()).isTrue();

        assertThat(response.confirmacaoPresenca().totalConfirmados()).isEqualTo(3);
        assertThat(response.confirmacaoPresenca().totalCancelados()).isEqualTo(1);
        assertThat(response.confirmacaoPresenca().totalPendentes()).isEqualTo(2);
        assertThat(response.confirmacaoPresenca().totalLembretesEnviados()).isEqualTo(6);
        assertThat(response.confirmacaoPresenca().percentualConfirmacao()).isEqualTo(75);

        assertThat(response.demandaPorEspecialidade()).extracting(DashboardResponse.DemandaPorEspecialidade::especialidade)
                .containsExactlyInAnyOrder("Cardiologia", "Dermatologia");
    }
}
