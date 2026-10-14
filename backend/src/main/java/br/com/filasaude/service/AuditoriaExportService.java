package br.com.filasaude.service;

import br.com.filasaude.domain.LogAuditoria;
import br.com.filasaude.dto.auditoria.ExportacaoArquivo;
import br.com.filasaude.dto.auditoria.LogAuditoriaResponse;
import br.com.filasaude.repository.LogAuditoriaRepository;
import br.com.filasaude.specification.LogAuditoriaSpecifications;
import br.com.filasaude.tenancy.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Roadmap #7 -- Trilha de auditoria exportável para TCE/MP. Exporta o log de
 * auditoria filtrado em PDF (relatório), CSV (planilha) ou JSON (dados), em
 * ordem cronológica, com um manifesto (quem gerou, quando, filtros, total).
 *
 * Integridade: o SHA-256 do arquivo gerado é devolvido ao usuário e gravado na
 * PRÓPRIA trilha (ação EXPORTAR_AUDITORIA) -- quem receber o arquivo depois pode
 * recalcular o hash e conferir com o registro. Isso prova que o arquivo não foi
 * alterado após a exportação; não substitui controles do banco de dados.
 *
 * Não é readOnly de propósito: a exportação em si é auditada.
 */
@Service
@Transactional
public class AuditoriaExportService {

    static final int MAX_REGISTROS = 50_000;
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;

    public AuditoriaExportService(LogAuditoriaRepository logAuditoriaRepository,
                                  AuditoriaService auditoriaService,
                                  ObjectMapper objectMapper) {
        this.logAuditoriaRepository = logAuditoriaRepository;
        this.auditoriaService = auditoriaService;
        this.objectMapper = objectMapper;
    }

    public ExportacaoArquivo exportar(String formato, String acao, Long usuarioId, LocalDate dataInicio,
                                      LocalDate dataFim, String entidade, Long entidadeId) {
        String fmt = formato == null ? "" : formato.toLowerCase();
        if (!List.of("pdf", "csv", "json").contains(fmt)) {
            throw new IllegalArgumentException("Formato inválido: use pdf, csv ou json.");
        }
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A data inicial não pode ser depois da data final.");
        }

        Specification<LogAuditoria> spec = Specification
                .where(LogAuditoriaSpecifications.comAcao(acao))
                .and(LogAuditoriaSpecifications.comUsuario(usuarioId))
                .and(LogAuditoriaSpecifications.apartirDe(dataInicio))
                .and(LogAuditoriaSpecifications.ate(dataFim))
                .and(LogAuditoriaSpecifications.comEntidade(entidade, entidadeId));

        long total = logAuditoriaRepository.count(spec);
        if (total > MAX_REGISTROS) {
            throw new IllegalStateException("A exportação teria " + total + " registros e o limite é " + MAX_REGISTROS
                    + ". Reduza o período ou use mais filtros.");
        }
        List<LogAuditoriaResponse> registros = logAuditoriaRepository
                .findAll(spec, Sort.by("criadoEm").ascending().and(Sort.by("id").ascending()))
                .stream().map(LogAuditoriaResponse::de).toList();

        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("periodo", periodo(dataInicio, dataFim));
        if (acao != null && !acao.isBlank()) filtros.put("acao", acao);
        if (usuarioId != null) filtros.put("usuarioId", String.valueOf(usuarioId));
        if (entidade != null && !entidade.isBlank()) {
            filtros.put("entidade", entidade + (entidadeId != null ? " #" + entidadeId : ""));
        }
        Manifesto manifesto = new Manifesto(LocalDateTime.now(), emailAutenticado(), prefeitura(), filtros, registros.size());

        byte[] conteudo;
        String contentType;
        switch (fmt) {
            case "csv" -> {
                conteudo = montarCsv(registros).getBytes(StandardCharsets.UTF_8);
                contentType = "text/csv; charset=UTF-8";
            }
            case "json" -> {
                conteudo = montarJson(manifesto, registros);
                contentType = "application/json";
            }
            default -> {
                conteudo = montarPdf(manifesto, registros);
                contentType = "application/pdf";
            }
        }

        String hash = RelatorioPdf.sha256(conteudo);
        String nome = "trilha-auditoria-" + LocalDate.now() + "." + fmt;
        registrarExportacao("EXPORTAR_AUDITORIA", "formato=" + fmt + "; registros=" + registros.size()
                + "; filtros=" + filtros + "; sha256=" + hash);
        return new ExportacaoArquivo(conteudo, nome, contentType, hash, registros.size());
    }

    public record Manifesto(LocalDateTime geradoEm, String geradoPorEmail, String prefeitura,
                     Map<String, String> filtros, long totalRegistros) {
    }

    private byte[] montarJson(Manifesto manifesto, List<LogAuditoriaResponse> registros) {
        try {
            Map<String, Object> raiz = new LinkedHashMap<>();
            raiz.put("manifesto", manifesto);
            raiz.put("registros", registros);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(raiz);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o JSON da auditoria", e);
        }
    }

    String montarCsv(List<LogAuditoriaResponse> registros) {
        StringBuilder sb = new StringBuilder("﻿id;data_hora;usuario_nome;usuario_email;acao;entidade;entidade_id;detalhe\n");
        for (LogAuditoriaResponse r : registros) {
            sb.append(TransparenciaService.celulaCsv(String.valueOf(r.id()))).append(';')
                    .append(TransparenciaService.celulaCsv(r.criadoEm() != null ? r.criadoEm().format(DATA_HORA) : "")).append(';')
                    .append(TransparenciaService.celulaCsv(r.usuarioNome())).append(';')
                    .append(TransparenciaService.celulaCsv(r.usuarioEmail())).append(';')
                    .append(TransparenciaService.celulaCsv(r.acao())).append(';')
                    .append(TransparenciaService.celulaCsv(r.entidade())).append(';')
                    .append(TransparenciaService.celulaCsv(r.entidadeId() != null ? String.valueOf(r.entidadeId()) : "")).append(';')
                    .append(TransparenciaService.celulaCsv(r.detalhe())).append('\n');
        }
        return sb.toString();
    }

    private byte[] montarPdf(Manifesto m, List<LogAuditoriaResponse> registros) {
        try {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 48);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, saida);
            writer.setPageEvent(RelatorioPdf.numeracao());
            document.open();

            document.add(new Paragraph("Trilha de auditoria — Fila de Regulação SUS", RelatorioPdf.TITULO));
            document.add(new Paragraph((m.prefeitura() != null ? m.prefeitura() + " · " : "")
                    + "gerado em " + m.geradoEm().format(DATA_HORA)
                    + (m.geradoPorEmail() != null ? " por " + m.geradoPorEmail() : ""), RelatorioPdf.SUBTITULO));
            Paragraph filtros = new Paragraph("Filtros: " + m.filtros() + " · Total de registros: " + m.totalRegistros(),
                    RelatorioPdf.SUBTITULO);
            filtros.setSpacingAfter(10);
            document.add(filtros);

            PdfPTable tabela = new PdfPTable(5);
            tabela.setWidthPercentage(100);
            tabela.setWidths(new float[]{1.5f, 2.2f, 2f, 1.4f, 5f});
            tabela.setHeaderRows(1);
            for (String c : new String[]{"Data/hora", "Usuário", "Ação", "Entidade", "Detalhe"}) {
                tabela.addCell(RelatorioPdf.cabecalho(c));
            }
            for (LogAuditoriaResponse r : registros) {
                tabela.addCell(RelatorioPdf.celula(r.criadoEm() != null ? r.criadoEm().format(DATA_HORA) : null));
                tabela.addCell(RelatorioPdf.celula(r.usuarioNome() != null ? r.usuarioNome() : r.usuarioEmail()));
                tabela.addCell(RelatorioPdf.celula(r.acao()));
                tabela.addCell(RelatorioPdf.celula(r.entidade() == null ? null
                        : r.entidade() + (r.entidadeId() != null ? " #" + r.entidadeId() : "")));
                tabela.addCell(RelatorioPdf.celula(r.detalhe()));
            }
            if (registros.isEmpty()) {
                document.add(new Paragraph("Nenhum registro para os filtros informados.", RelatorioPdf.TEXTO));
            } else {
                document.add(tabela);
            }

            Paragraph nota = new Paragraph("Integridade: o SHA-256 deste arquivo é exibido na tela no momento da exportação "
                    + "e gravado nesta mesma trilha (ação EXPORTAR_AUDITORIA, em " + m.geradoEm().format(DATA) + "). "
                    + "Recalcule o hash do arquivo recebido e compare com esse registro para comprovar que ele não foi alterado.",
                    RelatorioPdf.NOTA);
            nota.setSpacingBefore(12);
            document.add(nota);

            document.close();
            return saida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o PDF da auditoria", e);
        }
    }

    private void registrarExportacao(String acao, String detalhe) {
        String d = detalhe.length() > 500 ? detalhe.substring(0, 500) : detalhe;
        auditoriaService.registrar(acao, "LogAuditoria", null, d);
    }

    private static String periodo(LocalDate ini, LocalDate fim) {
        if (ini == null && fim == null) return "todo o histórico";
        return (ini != null ? ini.format(DATA) : "início") + " a " + (fim != null ? fim.format(DATA) : "hoje");
    }

    private static String emailAutenticado() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? a.getName() : null;
    }

    private static String prefeitura() {
        return TenantContext.getCurrentTenant();
    }
}
