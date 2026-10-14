package br.com.filasaude.service;

import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.dashboard.PrevisaoSlaResponse;
import br.com.filasaude.repository.HistoricoStatusRepository;
import br.com.filasaude.repository.ProcedimentoRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.repository.SlaConfiguracaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static br.com.filasaude.support.Fixtures.paciente;
import static br.com.filasaude.support.Fixtures.procedimento;
import static br.com.filasaude.support.Fixtures.protocolo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Dashboard preditivo de SLA (roadmap #5): vazão recente -> quem vai estourar o prazo. */
@ExtendWith(MockitoExtension.class)
class PrevisaoSlaServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 14);

    @Mock private ProtocoloRepository protocoloRepository;
    @Mock private HistoricoStatusRepository historicoStatusRepository;
    @Mock private SlaConfiguracaoRepository slaConfiguracaoRepository;
    @Mock private ProcedimentoRepository procedimentoRepository;
    @Mock private AuditoriaService auditoriaService;

    private final Paciente paciente = paciente(1L, "Paciente Teste");
    private final Procedimento ortopedia = procedimento(1L, "Cirurgia Ortopédica", "Ortopedia");
    private final Procedimento exame = procedimento(2L, "Ecocardiograma", "Cardiologia");

    private PrevisaoSlaService service() {
        lenient().when(slaConfiguracaoRepository.findAll()).thenReturn(List.of());
        SlaConfiguracaoService sla = new SlaConfiguracaoService(
                slaConfiguracaoRepository, procedimentoRepository, auditoriaService, 7, 15);
        return new PrevisaoSlaService(protocoloRepository, historicoStatusRepository,
                new FilaPriorizacaoService(protocoloRepository), sla);
    }

    private Protocolo aguardando(Long id, Procedimento procedimento, int diasEmEspera) {
        Protocolo p = protocolo(id, paciente, procedimento);
        p.setStatus(StatusProtocolo.AGUARDANDO);
        p.setDataInclusao(HOJE.minusDays(diasEmEspera));
        p.setCriadoEm(LocalDateTime.of(2026, 1, 1, 0, 0).plusMinutes(id));
        return p;
    }

    private void saidas(Object[]... linhas) {
        when(historicoStatusRepository.contarSaidasDaFila(any(), any(), any())).thenReturn(List.of(linhas));
    }

    private void entradas(Object[]... linhas) {
        when(protocoloRepository.contarEntradasDesde(any())).thenReturn(List.of(linhas));
    }

    @Test
    void classificaJaAtrasadoVaiAtrasarESemHistorico() {
        Protocolo atrasado = aguardando(1L, ortopedia, 20);
        Protocolo b = aguardando(2L, ortopedia, 13);
        Protocolo a = aguardando(3L, ortopedia, 10);
        Protocolo semBase = aguardando(4L, exame, 3);
        when(protocoloRepository.findByStatus(StatusProtocolo.AGUARDANDO)).thenReturn(List.of(a, b, atrasado, semBase));
        // 45 saídas em 90 dias = 0,5 protocolo/dia para a ortopedia; nenhuma para o exame.
        saidas(new Object[]{1L, 45L});
        entradas(new Object[]{1L, 60L}, new Object[]{2L, 2L});

        PrevisaoSlaResponse r = service().gerar(HOJE);

        assertThat(r.janelaDias()).isEqualTo(90);
        assertThat(r.resumo().totalAguardando()).isEqualTo(4);
        assertThat(r.resumo().jaAtrasados()).isEqualTo(1);
        assertThat(r.resumo().vaoAtrasar()).isEqualTo(2);
        assertThat(r.resumo().semHistorico()).isEqualTo(1);
        assertThat(r.resumo().percentualPrevistoNoPrazo()).isZero();

        // Ordem da fila: atrasado (20d) pos 1, b (13d) pos 2, a (10d) pos 3.
        // b: ceil(2/0,5)=4 dias -> 17 > 15 (vai atrasar em 3 dias); a: 6 dias -> 16 > 15 (em 6 dias).
        assertThat(r.protocolosEmRisco()).hasSize(2);
        assertThat(r.protocolosEmRisco().get(0).protocoloId()).isEqualTo(2L);
        assertThat(r.protocolosEmRisco().get(0).diasAteAtraso()).isEqualTo(3);
        assertThat(r.protocolosEmRisco().get(0).atendimentoEstimadoEmDias()).isEqualTo(4);
        assertThat(r.protocolosEmRisco().get(1).protocoloId()).isEqualTo(3L);
        assertThat(r.protocolosEmRisco().get(1).diasAteAtraso()).isEqualTo(6);

        PrevisaoSlaResponse.PrevisaoProcedimento linhaOrtopedia = r.porProcedimento().get(0);
        assertThat(linhaOrtopedia.procedimentoId()).isEqualTo(1L);
        assertThat(linhaOrtopedia.vazaoSemanal()).isEqualTo(3.5);
        assertThat(linhaOrtopedia.diasParaEsvaziarFila()).isEqualTo(6);
        assertThat(linhaOrtopedia.tendencia()).isEqualTo("CRESCENDO");

        PrevisaoSlaResponse.PrevisaoProcedimento linhaExame = r.porProcedimento().get(1);
        assertThat(linhaExame.diasParaEsvaziarFila()).isNull();
    }

    @Test
    void protocoloDentroDoRitmoFicaNoPrazo() {
        when(protocoloRepository.findByStatus(StatusProtocolo.AGUARDANDO)).thenReturn(List.of(aguardando(1L, ortopedia, 2)));
        saidas(new Object[]{1L, 90L}); // 1 por dia
        entradas(new Object[]{1L, 90L});

        PrevisaoSlaResponse r = service().gerar(HOJE);

        assertThat(r.resumo().vaoAtrasar()).isZero();
        assertThat(r.resumo().jaAtrasados()).isZero();
        assertThat(r.resumo().percentualPrevistoNoPrazo()).isEqualTo(100);
        assertThat(r.protocolosEmRisco()).isEmpty();
        assertThat(r.porProcedimento().get(0).tendencia()).isEqualTo("ESTAVEL");
    }

    @Test
    void semNinguemAguardandoNaoHaPrevisao() {
        when(protocoloRepository.findByStatus(StatusProtocolo.AGUARDANDO)).thenReturn(List.of());
        saidas();
        entradas();

        PrevisaoSlaResponse r = service().gerar(HOJE);

        assertThat(r.resumo().totalAguardando()).isZero();
        assertThat(r.resumo().percentualPrevistoNoPrazo()).isNull();
        assertThat(r.porProcedimento()).isEmpty();
        assertThat(r.protocolosEmRisco()).isEmpty();
    }
}
