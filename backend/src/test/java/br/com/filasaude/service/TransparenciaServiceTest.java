package br.com.filasaude.service;

import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.TransparenciaConfig;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.transparencia.TransparenciaConfigRequest;
import br.com.filasaude.dto.transparencia.TransparenciaResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.HistoricoStatusRepository;
import br.com.filasaude.repository.HorarioAgendaRepository;
import br.com.filasaude.repository.ProcedimentoRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.repository.SlaConfiguracaoRepository;
import br.com.filasaude.repository.TransparenciaConfigRepository;
import br.com.filasaude.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static br.com.filasaude.support.Fixtures.paciente;
import static br.com.filasaude.support.Fixtures.procedimento;
import static br.com.filasaude.support.Fixtures.protocolo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Transparência ativa: agregação, supressão de grupos pequenos (LGPD), publicação e exportação. */
@ExtendWith(MockitoExtension.class)
class TransparenciaServiceTest {

    @Mock private TransparenciaConfigRepository configRepository;
    @Mock private ProtocoloRepository protocoloRepository;
    @Mock private HistoricoStatusRepository historicoStatusRepository;
    @Mock private HorarioAgendaRepository horarioAgendaRepository;
    @Mock private SlaConfiguracaoRepository slaConfiguracaoRepository;
    @Mock private ProcedimentoRepository procedimentoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;

    private final Paciente paciente = paciente(1L, "Fulana Sigilosa da Silva");
    private final Procedimento ortopedia = procedimento(1L, "Cirurgia Ortopédica", "Ortopedia");

    private TransparenciaService service() {
        lenient().when(slaConfiguracaoRepository.findAll()).thenReturn(List.of());
        SlaConfiguracaoService sla = new SlaConfiguracaoService(
                slaConfiguracaoRepository, procedimentoRepository, auditoriaService, 7, 15);
        FilaPriorizacaoService fila = new FilaPriorizacaoService(protocoloRepository);
        PrevisaoSlaService previsao = new PrevisaoSlaService(protocoloRepository, historicoStatusRepository, fila, sla);
        return new TransparenciaService(configRepository, fila, sla, previsao, protocoloRepository,
                horarioAgendaRepository, usuarioRepository, auditoriaService);
    }

    private void filaComEsperas(int... dias) {
        List<Protocolo> lista = new ArrayList<>();
        long id = 1;
        for (int d : dias) {
            Protocolo p = protocolo(id, paciente, ortopedia);
            p.setStatus(StatusProtocolo.AGUARDANDO);
            p.setDataInclusao(LocalDate.now().minusDays(d));
            p.setCriadoEm(LocalDateTime.of(2026, 1, 1, 0, 0).plusMinutes(id));
            lista.add(p);
            id++;
        }
        when(protocoloRepository.findByStatus(StatusProtocolo.AGUARDANDO)).thenReturn(lista);
    }

    private TransparenciaConfig publicada() {
        return TransparenciaConfig.builder().publicado(true).build();
    }

    @Test
    void grupoPequenoEhSuprimidoENaoExpoeDadosDePaciente() {
        filaComEsperas(3, 20);

        TransparenciaResponse r = service().montar(publicada(), LocalDate.now());

        assertThat(r.fila().totalAguardando().rotulo()).isEqualTo("menos de 5");
        assertThat(r.fila().totalAguardando().valor()).isNull();
        assertThat(r.fila().percentualDentroDoPrazo()).isNull();
        assertThat(r.fila().porProcedimento()).hasSize(1);
        assertThat(r.fila().porProcedimento().get(0).aguardando().rotulo()).isEqualTo("menos de 5");
        assertThat(r.fila().porProcedimento().get(0).esperaMediaDias()).isNull();
        assertThat(r.fila().porProcedimento().get(0).esperaMaximaDias()).isNull();
        assertThat(r.toString()).doesNotContain("Fulana").doesNotContain("PROTO-");
    }

    @Test
    void grupoGrandePublicaContagemEsperaEPercentuais() {
        // limites padrão do teste: atenção > 7 dias, atrasado > 15 dias
        filaComEsperas(1, 2, 3, 10, 20); // 3 dentro, 1 em atenção, 1 fora

        TransparenciaResponse r = service().montar(publicada(), LocalDate.now());

        assertThat(r.fila().totalAguardando().valor()).isEqualTo(5);
        assertThat(r.fila().percentualDentroDoPrazo()).isEqualTo(60);
        assertThat(r.fila().percentualEmAtencao()).isEqualTo(20);
        assertThat(r.fila().percentualForaDoPrazo()).isEqualTo(20);
        assertThat(r.fila().porProcedimento().get(0).esperaMediaDias()).isEqualTo(7); // (1+2+3+10+20)/5 = 7,2
        assertThat(r.fila().porProcedimento().get(0).esperaMaximaDias()).isEqualTo(20);
    }

    @Test
    void secoesDesligadasNaoSaem() {
        TransparenciaConfig c = publicada();
        c.setMostrarFila(false);
        c.setMostrarAtendimentos(false);
        c.setMostrarVagas(false);
        c.setMostrarCriterios(false);

        TransparenciaResponse r = service().montar(c, LocalDate.now());

        assertThat(r.fila()).isNull();
        assertThat(r.atendimentos()).isNull();
        assertThat(r.vagas()).isNull();
        assertThat(r.criterios()).isNull();
    }

    @Test
    void atendimentosCobremDozeMesesEZerosNaoSaoSuprimidos() {
        LocalDate hoje = LocalDate.of(2026, 10, 14);
        when(protocoloRepository.contarConcluidosPorMes(any()))
                .thenReturn(List.<Object[]>of(new Object[]{2026, 9, 3L}, new Object[]{2026, 8, 12L}));
        TransparenciaConfig c = publicada();
        c.setMostrarFila(false);

        TransparenciaResponse r = service().montar(c, hoje);

        assertThat(r.atendimentos()).hasSize(12);
        assertThat(r.atendimentos().get(0).mes()).isEqualTo("2025-11");
        assertThat(r.atendimentos().get(11).mes()).isEqualTo(YearMonth.of(2026, 10).toString());
        assertThat(r.atendimentos().get(11).concluidos().rotulo()).isEqualTo("0");
        assertThat(r.atendimentos().get(10).concluidos().rotulo()).isEqualTo("menos de 5"); // setembro: 3
        assertThat(r.atendimentos().get(9).concluidos().rotulo()).isEqualTo("12");
    }

    @Test
    void paginaNaoPublicadaResponde404() {
        when(configRepository.findById(TransparenciaConfig.ID_UNICO))
                .thenReturn(Optional.of(TransparenciaConfig.builder().publicado(false).build()));

        assertThatThrownBy(() -> service().gerarPublico()).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void paginaPublicadaUsaCacheCurto() {
        when(configRepository.findById(TransparenciaConfig.ID_UNICO)).thenReturn(Optional.of(publicada()));
        when(protocoloRepository.findByStatus(StatusProtocolo.AGUARDANDO)).thenReturn(List.of());
        TransparenciaService s = service();

        TransparenciaResponse a = s.gerarPublico();
        TransparenciaResponse b = s.gerarPublico();

        assertThat(b).isSameAs(a);
        verify(configRepository, times(1)).findById(TransparenciaConfig.ID_UNICO);
    }

    @Test
    void publicarRegistraAuditoriaEInvalidaCache() {
        TransparenciaConfig c = TransparenciaConfig.builder().publicado(false).build();
        when(configRepository.findById(TransparenciaConfig.ID_UNICO)).thenReturn(Optional.of(c));

        var resp = service().atualizarConfig(new TransparenciaConfigRequest(
                true, true, true, true, true, "  Texto  ", " ouvidoria@prefeitura.gov.br "));

        assertThat(resp.publicado()).isTrue();
        assertThat(resp.textoIntroducao()).isEqualTo("Texto");
        verify(auditoriaService).registrar(eq("PUBLICAR_TRANSPARENCIA"), anyString(), any(), anyString());
    }

    @Test
    void csvTemBomNeutralizaFormulasENaoVazaPaciente() {
        filaComEsperas(1, 2);
        TransparenciaService s = service();
        TransparenciaResponse r = s.montar(publicada(), LocalDate.now());

        String csv = s.paraCsv(r);

        assertThat(csv).startsWith("﻿secao;indicador;detalhe;valor\n");
        assertThat(csv).contains("Fila;Aguardando atendimento;total;menos de 5");
        assertThat(csv).doesNotContain("Fulana");
        assertThat(TransparenciaService.celulaCsv("=HYPERLINK(\"x\")")).startsWith("\"'=");
        assertThat(TransparenciaService.celulaCsv("a;b")).isEqualTo("\"a;b\"");
        assertThat(TransparenciaService.celulaCsv("-1+2")).isEqualTo("'-1+2");
    }
}
