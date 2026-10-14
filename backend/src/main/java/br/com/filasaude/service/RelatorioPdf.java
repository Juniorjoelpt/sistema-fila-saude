package br.com.filasaude.service;

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Document;
import com.lowagie.text.Phrase;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Peças comuns dos relatórios em PDF de auditoria (fontes, células, numeração de página, SHA-256). */
final class RelatorioPdf {

    static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(0x1F, 0x38, 0x64));
    static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
    static final Font ROTULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);
    static final Font TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    static final Font SECAO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(0x1F, 0x38, 0x64));
    static final Font NOTA = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.GRAY);

    private RelatorioPdf() {
    }

    static PdfPCell cabecalho(String texto) {
        PdfPCell cela = new PdfPCell(new Paragraph(texto, ROTULO));
        cela.setBackgroundColor(new Color(0xEA, 0xF1, 0xF0));
        cela.setPadding(4);
        cela.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cela;
    }

    static PdfPCell celula(String texto) {
        PdfPCell cela = new PdfPCell(new Paragraph(texto == null || texto.isBlank() ? "—" : texto, TEXTO));
        cela.setPadding(3);
        return cela;
    }

    /** Escreve "Página N" no rodapé de cada página. */
    static PdfPageEventHelper numeracao() {
        return new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                        new Phrase("Página " + writer.getPageNumber(), NOTA),
                        (document.right() + document.left()) / 2, document.bottom() - 18, 0);
            }
        };
    }

    static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
