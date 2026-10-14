package br.com.filasaude.service;

import br.com.filasaude.domain.HistoricoPrioridade;
import br.com.filasaude.domain.HistoricoStatus;
import br.com.filasaude.domain.LogAuditoria;
import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.CategoriaPrioridade;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.auditoria.DossieProtocoloResponse;
import br.com.filasaude.dto.auditoria.ExportacaoArquivo;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.HistoricoPrioridadeRepository;
import br.com.filasaude.repository.HistoricoStatusRepository;
import br.com.filasaude.repository.LogAuditoriaRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static br.com.filasaude.support.Fixtures.paciente;
import static br.com.filasaude.support.Fixtures.procedimento;
import static br.com.filasaude.support.Fixtures.protocolo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Dossiê de auditoria de um protocolo (roadmap #7). */
@ExtendWith(MockitoExtension.class)
class DossieProtocoloServiceTest {

    @Mock private ProtocoloRepository protocoloRepository;
    @Mock private HistoricoStatusRepository historicoStatusRepository;
    @Mock private HistoricoPrioridadeRepository historicoPrioridadeRepository;
    @Mock private LogAuditoriaRepository logAuditoriaRepository;
    @Mock private AuditoriaService auditoriaService;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Paciente paciente = paciente(1L, "Fulana de Tal");
    private final Procedimento procedimento = procedimento(1L, "Ecocardiograma", "Cardiologia");

    private DossieProtocoloService service() {
        return new DossieProtocoloService(protocoloRepository, historicoStatusRepository,
                historicoPrioridadeRepository, logAuditoriaRepository, auditoriaService, mapper);
    }

    private Protocolo protocoloComHistorico() {
        paciente.setCpf("52998224725");
        Protocolo p = protocolo(10L, paciente, procedimento);
        p.setCategoriaPrioridade(CategoriaPrioridade.URGENCIA);
        lenient().when(protocoloRepository.findById(10L)).thenReturn(Optional.of(p));
        when(historicoStatusRepository.findByProtocoloIdOrderByCriadoEmAsc(10L)).thenReturn(List.of(
                HistoricoStatus.builder().protocolo(p).statusNovo(StatusProtocolo.AGUARDANDO)
                        .observacao("Protocolo criado").criadoEm(LocalDateTime.of(2026, 10, 1, 8, 0)).build()));
        when(historicoPrioridadeRepository.findByProtocoloIdOrderByCriadoEmAsc(10L)).thenReturn(List.of(
                HistoricoPrioridade.builder().protocolo(p).prioridadeAnterior(CategoriaPrioridade.NORMAL)
                        .prioridadeNova(CategoriaPrioridade.URGENCIA).motivo("Risco clínico")
                        .criadoEm(LocalDateTime.of(2026, 10, 2, 8, 0)).build()));
        when(logAuditoriaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
                LogAuditoria.builder().id(1L).usuarioEmail("admin@teste.com").acao("CRIAR_PROTOCOLO")
                        .entidade("Protocolo").entidadeId(10L).detalhe("criado").build()));
        return p;
    }

    @Test
    void montaDossieComHistoricosEMascaraCpf() {
        Protocolo p = protocoloComHistorico();

        DossieProtocoloResponse d = service().montar(p);

        assertThat(d.protocolo().cpfMascarado()).isEqualTo("***.982.247-**");
        assertThat(d.protocolo().categoriaPrioridade()).isEqualTo("URGENCIA");
        assertThat(d.historicoStatus()).hasSize(1);
        assertThat(d.historicoPrioridade().get(0).motivo()).isEqualTo("Risco clínico");
        assertThat(d.trilhaAuditoria()).hasSize(1);
        assertThat(d.manifesto().totalEventos()).isEqualTo(3);
    }

    @Test
    void jsonNaoTrazCpfCompletoERegistraHashNaTrilha() throws Exception {
        protocoloComHistorico();

        ExportacaoArquivo a = service().gerar(10L, "json");

        String json = new String(a.conteudo(), StandardCharsets.UTF_8);
        assertThat(json).doesNotContain("52998224725").contains("***.982.247-**");
        assertThat(mapper.readTree(a.conteudo()).get("protocolo").get("numeroProtocolo").asText()).isEqualTo("PROTO-10");
        assertThat(a.nomeArquivo()).isEqualTo("dossie-PROTO-10.json");
        verify(auditoriaService).registrar(eq("EXPORTAR_DOSSIE_PROTOCOLO"), eq("Protocolo"), eq(10L),
                contains("sha256=" + a.sha256()));
    }

    @Test
    void pdfEhValido() {
        protocoloComHistorico();

        ExportacaoArquivo a = service().gerar(10L, "pdf");

        assertThat(new String(a.conteudo(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(a.sha256()).isEqualTo(RelatorioPdf.sha256(a.conteudo()));
    }

    @Test
    void protocoloInexistenteEFormatoInvalido() {
        when(protocoloRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().gerar(99L, "pdf")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service().gerar(10L, "csv")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mascararDocumentos() {
        assertThat(DossieProtocoloService.mascarar("52998224725")).isEqualTo("***.982.247-**");
        assertThat(DossieProtocoloService.mascarar("898001234567890")).isEqualTo("***********7890");
        assertThat(DossieProtocoloService.mascarar(null)).isNull();
        assertThat(DossieProtocoloService.mascarar("12")).isEqualTo("****");
    }
}
