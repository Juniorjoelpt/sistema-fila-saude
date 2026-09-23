package br.com.filasaude.service;

import br.com.filasaude.domain.EtapaProtocolo;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.ProtocoloRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Emissão de comprovante do protocolo em PDF (itens 3.1 e 3.5 do levantamento
 * de requisitos): documento que o cidadão baixa na consulta pública, com um
 * "selo de documento digital" simplificado — um hash de verificação derivado
 * dos dados do protocolo no momento da emissão, impresso no rodapé.
 *
 * Este não é um laudo médico (o sistema não armazena esse conteúdo clínico) —
 * é o comprovante de situação do protocolo na fila de regulação.
 */
@Service
@Transactional(readOnly = true)
public class ComprovanteService {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProtocoloRepository protocoloRepository;

    public ComprovanteService(ProtocoloRepository protocoloRepository) {
        this.protocoloRepository = protocoloRepository;
    }

    /**
     * Gera o PDF do comprovante, validando que o documento (CPF/CNS)
     * informado corresponde ao paciente do protocolo — mesma checagem de
     * posse usada na consulta pública, para que ninguém baixe o comprovante
     * de outra pessoa apenas sabendo o número do protocolo.
     */
    public byte[] gerar(String numeroProtocolo, String documentoBruto) {
        String documento = apenasDigitos(documentoBruto);
        Protocolo protocolo = protocoloRepository.findByNumeroProtocolo(numeroProtocolo)
                .orElseThrow(() -> new ResourceNotFoundException("Protocolo não encontrado: " + numeroProtocolo));

        String cpfPaciente = apenasDigitos(protocolo.getPaciente().getCpf());
        String cnsPaciente = apenasDigitos(protocolo.getPaciente().getCns());
        boolean pertenceAoPaciente = (!documento.isBlank())
                && (documento.equals(cpfPaciente) || documento.equals(cnsPaciente));
        if (!pertenceAoPaciente) {
            throw new ResourceNotFoundException("Protocolo não encontrado: " + numeroProtocolo);
        }

        return montarPdf(protocolo);
    }

    private byte[] montarPdf(Protocolo protocolo) {
        try {
            Document document = new Document(PageSize.A4, 56, 56, 56, 56);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, saida);
            document.open();

            Font fonteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(0x1F, 0x38, 0x64));
            Font fonteSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.GRAY);
            Font fonteRotulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
            Font fonteValor = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
            Font fonteRodape = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            document.add(new Paragraph("Comprovante de Protocolo — Fila de Regulação SUS", fonteTitulo));
            document.add(new Paragraph("Emitido em " + LocalDateTime.now().format(DATA_HORA), fonteSubtitulo));
            document.add(Chunk.NEWLINE);

            PdfPTable tabela = new PdfPTable(2);
            tabela.setWidthPercentage(100);
            tabela.setWidths(new float[]{1f, 2f});

            adicionarLinha(tabela, "Protocolo", protocolo.getNumeroProtocolo(), fonteRotulo, fonteValor);
            adicionarLinha(tabela, "Paciente", protocolo.getPaciente().getNome(), fonteRotulo, fonteValor);
            adicionarLinha(tabela, "Procedimento", protocolo.getProcedimento().getNome(), fonteRotulo, fonteValor);
            adicionarLinha(tabela, "Unidade de saúde",
                    protocolo.getUnidadeSaude() != null ? protocolo.getUnidadeSaude().getNome() : "A definir",
                    fonteRotulo, fonteValor);
            adicionarLinha(tabela, "Status atual", protocolo.getStatus().name(), fonteRotulo, fonteValor);
            adicionarLinha(tabela, "Data de solicitação", protocolo.getDataSolicitacao().format(DATA), fonteRotulo, fonteValor);
            adicionarLinha(tabela, "Entrada na fila", protocolo.getDataInclusao().format(DATA), fonteRotulo, fonteValor);
            if (protocolo.getDataPrevista() != null) {
                adicionarLinha(tabela, "Data prevista", protocolo.getDataPrevista().format(DATA), fonteRotulo, fonteValor);
            }

            document.add(tabela);
            document.add(Chunk.NEWLINE);

            if (!protocolo.getEtapas().isEmpty()) {
                document.add(new Paragraph("Linha do tempo do atendimento", fonteRotulo));
                document.add(Chunk.NEWLINE);

                PdfPTable etapasTabela = new PdfPTable(3);
                etapasTabela.setWidthPercentage(100);
                etapasTabela.setWidths(new float[]{3f, 1.5f, 1.5f});
                adicionarCabecalho(etapasTabela, "Etapa", fonteRotulo);
                adicionarCabecalho(etapasTabela, "Situação", fonteRotulo);
                adicionarCabecalho(etapasTabela, "Data", fonteRotulo);

                for (EtapaProtocolo etapa : protocolo.getEtapas()) {
                    etapasTabela.addCell(celaSimples(etapa.getNomeEtapa(), fonteValor));
                    etapasTabela.addCell(celaSimples(etapa.getStatus().name(), fonteValor));
                    etapasTabela.addCell(celaSimples(
                            etapa.getDataRealizacao() != null ? etapa.getDataRealizacao().format(DATA) : "—",
                            fonteValor));
                }
                document.add(etapasTabela);
                document.add(Chunk.NEWLINE);
            }

            Paragraph selo = new Paragraph();
            selo.add(new Chunk("Selo de documento digital: ", fonteRodape));
            selo.add(new Chunk(gerarSeloVerificacao(protocolo), fonteRodape));
            selo.setSpacingBefore(20);
            document.add(selo);

            document.add(new Paragraph(
                    "Este comprovante reflete a situação do protocolo no momento da emissão e não substitui "
                            + "laudo ou prescrição médica. Em caso de dúvida sobre sua autenticidade, consulte "
                            + "novamente o protocolo no portal informando o mesmo CPF/CNS.",
                    fonteRodape));

            document.close();
            return saida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o comprovante em PDF", e);
        }
    }

    private void adicionarLinha(PdfPTable tabela, String rotulo, String valor, Font fonteRotulo, Font fonteValor) {
        PdfPCell celaRotulo = new PdfPCell(new Paragraph(rotulo, fonteRotulo));
        celaRotulo.setBorder(0);
        celaRotulo.setPaddingBottom(6);
        tabela.addCell(celaRotulo);

        PdfPCell celaValor = new PdfPCell(new Paragraph(valor != null ? valor : "—", fonteValor));
        celaValor.setBorder(0);
        celaValor.setPaddingBottom(6);
        tabela.addCell(celaValor);
    }

    private void adicionarCabecalho(PdfPTable tabela, String texto, Font fonte) {
        PdfPCell cela = new PdfPCell(new Paragraph(texto, fonte));
        cela.setBackgroundColor(new Color(0xEA, 0xF1, 0xF0));
        cela.setPadding(6);
        cela.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabela.addCell(cela);
    }

    private PdfPCell celaSimples(String texto, Font fonte) {
        PdfPCell cela = new PdfPCell(new Paragraph(texto != null ? texto : "—", fonte));
        cela.setPadding(6);
        return cela;
    }

    /**
     * Hash simples (SHA-256, truncado) dos dados essenciais do protocolo no
     * momento da emissão — não é uma assinatura digital criptográfica
     * completa (ICP-Brasil), mas dá um selo de verificação leve, coerente com
     * o estágio atual do produto (item 3.5 do levantamento).
     */
    private String gerarSeloVerificacao(Protocolo protocolo) {
        try {
            String base = String.join("|",
                    protocolo.getNumeroProtocolo(),
                    protocolo.getStatus().name(),
                    String.valueOf(protocolo.getAtualizadoEm()));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                hex.append(String.format(Locale.ROOT, "%02x", hash[i]));
            }
            return hex.toString().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "INDISPONIVEL";
        }
    }

    private String apenasDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
