package br.com.filasaude.tenancy;

/** Binário de um logo de tenant enviado por upload (item 3.6 do levantamento de requisitos). */
public record TenantLogo(byte[] dados, String contentType) {
}
