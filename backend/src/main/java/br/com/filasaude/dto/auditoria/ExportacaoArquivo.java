package br.com.filasaude.dto.auditoria;

/** Arquivo de exportação pronto para download, com o SHA-256 do conteúdo (registrado também na trilha). */
public record ExportacaoArquivo(byte[] conteudo, String nomeArquivo, String contentType, String sha256, long registros) {
}
