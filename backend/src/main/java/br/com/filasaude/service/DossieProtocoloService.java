package br.com.filasaude.service;

import br.com.filasaude.domain.EtapaProtocolo;
import br.com.filasaude.domain.HistoricoPrioridade;
import br.com.filasaude.domain.HistoricoStatus;
import br.com.filasaude.domain.LogAuditoria;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.dto.auditoria.DossieProtocoloResponse;
import br.com.filasaude.dto.auditoria.ExportacaoArquivo;
import br.com.filasaude.dto.auditoria.LogAuditoriaResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.HistoricoPrioridadeRepository;
import br.com.filasaude.repository.HistoricoStatusRepository;
import br.com.filasaude.repository.LogAuditoriaRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.specification.LogAuditoriaSpecifications;
import br.com.filasaude.tenancy.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Roadmap #7 -- dossiê de um protocolo para TCE/MP: tudo o que o sistema sabe sobre
 * a história de UM protocolo (status, prioridade, etapas e trilha de auditoria),
 * em PDF ou JSON, com o SHA-256 do arquivo gravado na trilha. CPF/CNS mascarados.
 * Não é readOnly: a geração do dossiê é auditada.
 */
@Service
@Transactional
public class DossieProtocoloService {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProtocoloRepository protocoloRepository;
    private final HistoricoStatusRepository historicoStatusRepository;
    private final HistoricoPrioridadeRepository historicoPrioridadeRepository;
    private final LogAuditoriaRepository logAuditoriaRepository;
    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;

    public DossieProtocoloService(ProtocoloRepository protocoloRepository,
                                  HistoricoStatusRepository historicoStatusRepository,
                                  HistoricoPrioridadeRepository historicoPrioridadeRepository,
                                  LogAuditoriaRepository logAuditoriaRepository,
                                  AuditoriaService auditoriaService,
                                  ObjectMapper objectMapper) {
        this.protocoloRepository = protocoloRepository;
        this.historicoStatusRepository = historicoStatusRepository;
        this.historicoPrioridadeRepository = historicoPrioridadeRepository;
        this.logAuditoriaRepository = logAuditoriaRepository;
        this.auditoriaService = auditoriaService;
        this.objectMapper = objectMapper;
    }

    public ExportacaoArquivo gerar(Long protocoloId, String formato) {
        String fmt = formato == null ? "" : formato.toLowerCase();
        if (!List.of("pdf", "json").contains(fmt)) {
            throw new IllegalArgumentException("Formato inválido: use pdf ou json.");
        }
        Protocolo p = protocoloRepository.findById(protocoloId)
                .orElseThrow(() -> new ResourceNotFoundException("Protocolo não encontrado: " + protocoloId));

        DossieProtocoloResponse dossie = montar(p);
        byte[] conteudo;
        String contentType;
        if (fmt.equals("json")) {
            try {
                conteudo = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(dossie);
            } catch (Exception e) {
                throw new IllegalStateException("Não foi possível gerar o JSON do dossiê", e);
            }
            contentType = "application/json";
        } else {
            conteudo = montarPdf(dossie);
            contentType = "application/pdf";
        }

        String hash = RelatorioPdf.sha256(conteudo);
        auditoriaService.registrar("EXPORTAR_DOSSIE_PROTOCOLO", "Protocolo", p.getId(),
                "Dossiê do protocolo " + p.getNumeroProtocolo() + "; formato=" + fmt
                        + "; eventos=" + dossie.manifesto().totalEventos() + "; sha256=" + hash);
        return new ExportacaoArquivo(conteudo, "dossie-" + p.getNumeroProtocolo() + "." + fmt, contentType, hash,
                dossie.manifesto().totalEventos());
    }

    DossieProtocoloResponse montar(Protocolo p) {
        List<DossieProtocoloResponse.Etapa> etapas = p.getEtapas().stream()
                .map((EtapaProtocolo e) -> new DossieProtocoloResponse.Etapa(
                        e.getOrdem(), e.getNomeEtapa(), e.getStatus().name(), e.getDataRealizacao()))
                .toList();
        List<DossieProtocoloResponse.MudancaStatus> status = historicoStatusRepository
                .findByProtocoloIdOrderByCriadoEmAsc(p.getId()).stream()
                .map((HistoricoStatus h) -> new DossieProtocoloResponse.MudancaStatus(
                        h.getStatusAnterior() != null ? h.getStatusAnterior().name() : null,
                        h.getStatusNovo().name(), h.getObservacao(), quem(h.getUsuario()), h.getCriadoEm()))
                .toList();
        List<DossieProtocoloResponse.MudancaPrioridade> prioridade = historicoPrioridadeRepository
                .findByProtocoloIdOrderByCriadoEmAsc(p.getId()).stream()
                .map((HistoricoPrioridade h) -> new DossieProtocoloResponse.MudancaPrioridade(
                        h.getPrioridadeAnterior().name(), h.getPrioridadeNova().name(), h.getMotivo(),
                        quem(h.getUsuario()), h.getCriadoEm()))
                .toList();
        List<LogAuditoriaResponse> trilha = logAuditoriaRepository
                .findAll(LogAuditoriaSpecifications.comEntidade("Protocolo", p.getId()),
                        Sort.by("criadoEm").ascending().and(Sort.by("id").ascending()))
                .stream().map((LogAuditoria l) -> LogAuditoriaResponse.de(l)).toList();

        DossieProtocoloResponse.Protocolo dados = new DossieProtocoloResponse.Protocolo(
                p.getId(), p.getNumeroProtocolo(), p.getPaciente().getNome(),
                mascarar(p.getPaciente().getCpf()), mascarar(p.getPaciente().getCns()),
                p.getProcedimento().getNome(), p.getProcedimento().getEspecialidade(),
                p.getUnidadeSaude() != null ? p.getUnidadeSaude().getNome() : null,
                p.getCategoriaPrioridade().name(),
                p.getCategoriaSolicitada() != null ? p.getCategoriaSolicitada().name() : null,
                p.getStatus().name(), p.getProcessoJudicial(), p.getDataSolicitacao(), p.getDataInclusao(),
                p.getDataPrevista(), p.getCriadoEm(), p.getAtualizadoEm());

        long eventos = (long) status.size() + prioridade.size() + trilha.size();
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return new DossieProtocoloResponse(
                new DossieProtocoloResponse.Manifesto(LocalDateTime.now(), a != null ? a.getName() : null,
                        TenantContext.getCurrentTenant(), eventos),
                dados, etapas, status, prioridade, trilha);
    }

    private static String quem(Usuario u) {
        if (u == null) return null;
        return u.getNome() != null ? u.getNome() + " <" + u.getEmail() + ">" : u.getEmail();
    }

    /** CPF (11 dígitos) vira ***.456.789-**; outros tamanhos mostram só os 4 últimos caracteres. */
    static String mascarar(String documento) {
        if (documento == null || documento.isBlank()) return null;
        String d = documento.replaceAll("\\D", "");
        if (d.length() == 11) {
            return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
        }
        return d.length() <= 4 ? "****" : "*".repeat(d.length() - 4) + d.substring(d.length() - 4);
    }

    private byte[] montarPdf(DossieProtocoloResponse d) {
        try {
            Document document = new Document(PageSize.A4, 40, 40, 40, 52);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, saida);
            writer.setPageEvent(RelatorioPdf.numeracao());
            document.open();

            DossieProtocoloResponse.Protocolo p = d.protocolo();
            document.add(new Paragraph("Dossiê de auditoria do protocolo " + p.numeroProtocolo(), RelatorioPdf.TITULO));
            Paragraph sub = new Paragraph((d.manifesto().prefeitura() != null ? d.manifesto().prefeitura() + " · " : "")
                    + "gerado em " + d.manifesto().geradoEm().format(DATA_HORA)
                    + (d.manifesto().geradoPorEmail() != null ? " por " + d.manifesto().geradoPorEmail() : ""),
                    RelatorioPdf.SUBTITULO);
            sub.setSpacingAfter(10);
            document.add(sub);

            document.add(new Paragraph("Dados do protocolo", RelatorioPdf.SECAO));
            PdfPTable dados = new PdfPTable(2);
            dados.setWidthPercentage(100);
            dados.setWidths(new float[]{1.2f, 3f});
            linha(dados, "Paciente", p.paciente());
            linha(dados, "CPF / CNS (mascarados)", (p.cpfMascarado() != null ? p.cpfMascarado() : "—") + " / "
                    + (p.cnsMascarado() != null ? p.cnsMascarado() : "—"));
            linha(dados, "Procedimento", p.procedimento() + (p.especialidade() != null ? " (" + p.especialidade() + ")" : ""));
            linha(dados, "Unidade de saúde", p.unidadeSaude());
            linha(dados, "Categoria / status", p.categoriaPrioridade() + " / " + p.status()
                    + (p.categoriaSolicitadaPendente() != null ? " (pedido de " + p.categoriaSolicitadaPendente() + " pendente de confirmação)" : ""));
            linha(dados, "Processo judicial", p.processoJudicial());
            linha(dados, "Solicitação / inclusão / previsão", fmt(p.dataSolicitacao()) + " / " + fmt(p.dataInclusao()) + " / " + fmt(p.dataPrevista()));
            dados.setSpacingAfter(10);
            document.add(dados);

            secao(document, "Histórico de status", new String[]{"Data/hora", "De → Para", "Usuário", "Observação"},
                    new float[]{1.6f, 2f, 2.4f, 4f},
                    d.historicoStatus().stream().map(h -> new String[]{h.em().format(DATA_HORA),
                            (h.de() != null ? h.de() : "—") + " → " + h.para(), h.usuario(), h.observacao()}).toList());
            secao(document, "Histórico de prioridade", new String[]{"Data/hora", "De → Para", "Usuário", "Motivo"},
                    new float[]{1.6f, 2f, 2.4f, 4f},
                    d.historicoPrioridade().stream().map(h -> new String[]{h.em().format(DATA_HORA),
                            h.de() + " → " + h.para(), h.usuario(), h.motivo()}).toList());
            secao(document, "Etapas", new String[]{"#", "Etapa", "Situação", "Realizada em"},
                    new float[]{0.5f, 4f, 1.6f, 1.6f},
                    d.etapas().stream().map(e -> new String[]{String.valueOf(e.ordem()), e.nome(), e.status(), fmt(e.dataRealizacao())}).toList());
            secao(document, "Trilha de auditoria geral", new String[]{"Data/hora", "Usuário", "Ação", "Detalhe"},
                    new float[]{1.6f, 2.4f, 2f, 4f},
                    d.trilhaAuditoria().stream().map(l -> new String[]{l.criadoEm().format(DATA_HORA),
                            l.usuarioNome() != null ? l.usuarioNome() : l.usuarioEmail(), l.acao(), l.detalhe()}).toList());

            Paragraph nota = new Paragraph("Integridade: o SHA-256 deste arquivo é exibido na tela no momento da geração e gravado "
                    + "na trilha de auditoria (ação EXPORTAR_DOSSIE_PROTOCOLO). Recalcule o hash do arquivo recebido e compare "
                    + "com esse registro para comprovar que ele não foi alterado.", RelatorioPdf.NOTA);
            nota.setSpacingBefore(12);
            document.add(nota);
            document.close();
            return saida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o PDF do dossiê", e);
        }
    }

    private static void secao(Document document, String titulo, String[] colunas, float[] larguras, List<String[]> linhas) throws Exception {
        Paragraph t = new Paragraph(titulo, RelatorioPdf.SECAO);
        t.setSpacingBefore(6);
        t.setSpacingAfter(4);
        document.add(t);
        if (linhas.isEmpty()) {
            document.add(new Paragraph("Sem registros.", RelatorioPdf.TEXTO));
            return;
        }
        PdfPTable tabela = new PdfPTable(colunas.length);
        tabela.setWidthPercentage(100);
        tabela.setWidths(larguras);
        tabela.setHeaderRows(1);
        for (String c : colunas) {
            tabela.addCell(RelatorioPdf.cabecalho(c));
        }
        for (String[] l : linhas) {
            for (String c : l) {
                tabela.addCell(RelatorioPdf.celula(c));
            }
        }
        document.add(tabela);
    }

    private static void linha(PdfPTable tabela, String rotulo, String valor) {
        PdfPCell r = new PdfPCell(new Paragraph(rotulo, RelatorioPdf.ROTULO));
        r.setBorder(0);
        r.setPaddingBottom(3);
        tabela.addCell(r);
        PdfPCell v = new PdfPCell(new Paragraph(valor == null || valor.isBlank() ? "—" : valor, RelatorioPdf.TEXTO));
        v.setBorder(0);
        v.setPaddingBottom(3);
        tabela.addCell(v);
    }

    private static String fmt(LocalDate d) {
        return d == null ? "—" : d.format(DATA);
    }
}
