package br.com.filasaude.dto.importacao;

/** Uma linha da planilha que não pôde ser importada, com o motivo. */
public record ImportacaoErro(int linha, String motivo) {
}
