package br.com.filasaude.tenancy;

/**
 * Representa um registro de prefeitura (tenant) armazenado no banco MASTER.
 * Cada tenant aponta para um banco de dados MySQL próprio e isolado
 * (decisão registrada no levantamento de requisitos: "banco de dados separado por tenant").
 */
public record TenantRecord(
        Long id,
        String slug,
        String nomeMunicipio,
        String dbHost,
        Integer dbPort,
        String dbName,
        String dbUser,
        String dbPassword,
        String corPrimaria,
        String corSecundaria,
        String logoUrl,
        boolean logoUpload,
        boolean ativo
) {
    public String jdbcUrl() {
        return "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Fortaleza"
                .formatted(dbHost, dbPort, dbName);
    }
}
