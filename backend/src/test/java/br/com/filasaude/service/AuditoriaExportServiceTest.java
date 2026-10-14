package br.com.filasaude.service;

import br.com.filasaude.domain.LogAuditoria;
import br.com.filasaude.dto.auditoria.ExportacaoArquivo;
import br.com.filasaude.repository.LogAuditoriaRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Exportação da trilha de auditoria (roadmap #7): formatos, manifesto, hash e auditoria da própria exportação. */
@ExtendWith(MockitoExtension.class)
class AuditoriaExportServiceTest {

    @Mock private LogAuditoriaRepository logAuditoriaRepository;
    @Mock private AuditoriaService auditoriaService;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AuditoriaExportService service() {
        return new AuditoriaExportService(logAuditoriaRepository, auditoriaService, mapper);
    }

    private LogAuditoria log(long id, String acao, String detalhe) {
        return LogAuditoria.builder().id(id).usuarioEmail("admin@teste.com").acao(acao).entidade("Protocolo")
                .entidadeId(7L).detalhe(detalhe).criadoEm(LocalDateTime.of(2026, 10, 1, 9, 30, 15)).build();
    }

    private void comRegistros(LogAuditoria... logs) {
        when(logAuditoriaRepository.count(any(Specification.class))).thenReturn((long) logs.length);
        when(logAuditoriaRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(logs));
    }

    @Test
    void csvTemBomCabecalhoLinhasEHashRegistradoNaTrilha() {
        comRegistros(log(1, "CRIAR_PROTOCOLO", "Protocolo PROT-1 criado"), log(2, "MUDAR_STATUS_PROTOCOLO", "=cmd|' /C calc'!A0"));

        ExportacaoArquivo a = service().exportar("csv", null, null, null, null, null, null);

        String csv = new String(a.conteudo(), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿id;data_hora;usuario_nome;usuario_email;acao;entidade;entidade_id;detalhe\n");
        assertThat(csv).contains("1;01/10/2026 09:30:15;;admin@teste.com;CRIAR_PROTOCOLO;Protocolo;7;Protocolo PROT-1 criado");
        assertThat(csv).contains(";'=cmd|' /C calc'!A0"); // fórmula neutralizada
        assertThat(a.registros()).isEqualTo(2);
        assertThat(a.nomeArquivo()).startsWith("trilha-auditoria-").endsWith(".csv");
        assertThat(a.sha256()).hasSize(64).isEqualTo(RelatorioPdf.sha256(a.conteudo()));
        verify(auditoriaService).registrar(eq("EXPORTAR_AUDITORIA"), eq("LogAuditoria"), any(),
                contains("sha256=" + a.sha256()));
    }

    @Test
    void jsonTemManifestoERegistros() throws Exception {
        comRegistros(log(1, "LOGIN", "ok"));

        ExportacaoArquivo a = service().exportar("json", "LOGIN", null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null);

        var raiz = mapper.readTree(a.conteudo());
        assertThat(raiz.get("manifesto").get("totalRegistros").asLong()).isEqualTo(1);
        assertThat(raiz.get("manifesto").get("filtros").get("acao").asText()).isEqualTo("LOGIN");
        assertThat(raiz.get("manifesto").get("filtros").get("periodo").asText()).isEqualTo("01/10/2026 a 31/10/2026");
        assertThat(raiz.get("registros")).hasSize(1);
        assertThat(raiz.get("registros").get(0).get("acao").asText()).isEqualTo("LOGIN");
        assertThat(a.contentType()).isEqualTo("application/json");
    }

    @Test
    void pdfEhUmPdfValido() {
        comRegistros(log(1, "LOGIN", "ok"), log(2, "CRIAR_PROTOCOLO", "x"));

        ExportacaoArquivo a = service().exportar("PDF", null, null, null, null, null, null);

        assertThat(new String(a.conteudo(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(a.contentType()).isEqualTo("application/pdf");
        assertThat(a.registros()).isEqualTo(2);
    }

    @Test
    void pdfSemRegistrosAindaGera() {
        comRegistros();

        ExportacaoArquivo a = service().exportar("pdf", null, null, null, null, null, null);

        assertThat(new String(a.conteudo(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(a.registros()).isZero();
    }

    @Test
    void formatoInvalidoEPeriodoInvertidoSaoRecusados() {
        assertThatThrownBy(() -> service().exportar("xml", null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Formato inválido");
        assertThatThrownBy(() -> service().exportar("csv", null, null, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("data inicial");
        verify(auditoriaService, never()).registrar(anyString(), anyString(), any(), anyString());
    }

    @Test
    void exportacaoGrandeDemaisEhRecusadaSemGerarArquivo() {
        when(logAuditoriaRepository.count(any(Specification.class))).thenReturn(50_001L);

        assertThatThrownBy(() -> service().exportar("csv", null, null, null, null, null, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("limite");
        verify(auditoriaService, never()).registrar(anyString(), anyString(), any(), anyString());
    }
}
