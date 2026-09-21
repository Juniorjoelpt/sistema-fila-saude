package br.com.filasaude.tenancy;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String slug) {
        super("Prefeitura (tenant) não encontrada ou inativa: " + slug);
    }
}
