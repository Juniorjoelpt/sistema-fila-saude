package br.com.filasaude.tenancy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

/**
 * Acesso à tabela `tenants` no banco MASTER (cadastro central de prefeituras).
 * Usa JdbcTemplate simples, propositalmente fora do Spring Data JPA: o JPA/Hibernate
 * principal da aplicação é dedicado às entidades de negócio, que residem nos bancos
 * de cada tenant, roteados dinamicamente (ver {@link TenantRoutingDataSource}).
 * Manter dois EntityManagerFactory (um para o master, outro por tenant) adicionaria
 * complexidade desnecessária para o MVP — a tabela de tenants é pequena e simples.
 *
 * O JdbcTemplate é construído aqui manualmente (não é exposto como @Bean do Spring)
 * de propósito: um bean JdbcTemplate ganha automaticamente, via
 * DependsOnDatabaseInitializationPostProcessor do Spring Boot, uma dependência
 * implícita do inicializador de schema (dataSourceScriptDatabaseInitializer), que
 * por sua vez depende do datasource @Primary (tenantRoutingDataSource) — e esse
 * datasource só é construído depois deste repositório (via TenantDataSourceRegistry).
 * Isso fecha um ciclo de beans. Mantendo o JdbcTemplate como um campo comum (não bean),
 * o ciclo nunca se forma.
 *
 * As colunas de binário do logo (logo_dados/logo_content_type) nunca são lidas nas
 * consultas de listagem/resolução de tenant abaixo -- só a flag calculada
 * "logo_upload" (se há ou não um arquivo enviado) --, para não carregar o BLOB inteiro
 * em toda consulta. O conteúdo do logo é buscado à parte, só quando servido (ver
 * {@link #buscarLogo(String)}).
 */
@Repository
public class MasterTenantRepository {

    private static final String COLUNAS_BASE = """
            id, slug, nome_municipio, db_host, db_port, db_name, db_user, db_password,
            cor_primaria, cor_secundaria, logo_url, (logo_dados IS NOT NULL) AS logo_upload, ativo
            """;

    private final JdbcTemplate masterJdbcTemplate;

    public MasterTenantRepository(@Qualifier("masterDataSource") DataSource masterDataSource) {
        this.masterJdbcTemplate = new JdbcTemplate(masterDataSource);
    }

    public List<TenantRecord> findAllAtivos() {
        return masterJdbcTemplate.query(
                "SELECT " + COLUNAS_BASE + " FROM tenants WHERE ativo = true",
                this::mapRow
        );
    }

    public Optional<TenantRecord> findBySlug(String slug) {
        List<TenantRecord> result = masterJdbcTemplate.query(
                "SELECT " + COLUNAS_BASE + " FROM tenants WHERE slug = ? AND ativo = true",
                this::mapRow,
                slug
        );
        return result.stream().findFirst();
    }

    /** Ignora o filtro de "ativo" — usado para checar unicidade de slug no provisionamento. */
    public Optional<TenantRecord> findAnyBySlug(String slug) {
        List<TenantRecord> result = masterJdbcTemplate.query(
                "SELECT " + COLUNAS_BASE + " FROM tenants WHERE slug = ?",
                this::mapRow,
                slug
        );
        return result.stream().findFirst();
    }

    /** Todas as prefeituras cadastradas, ativas ou não — painel de superadmin (item 3.6). */
    public List<TenantRecord> findAll() {
        return masterJdbcTemplate.query(
                "SELECT " + COLUNAS_BASE + " FROM tenants ORDER BY nome_municipio ASC",
                this::mapRow
        );
    }

    public void atualizarAtivo(String slug, boolean ativo) {
        masterJdbcTemplate.update("UPDATE tenants SET ativo = ? WHERE slug = ?", ativo, slug);
    }

    /** Atualiza a identidade visual (cores e/ou URL externa de logo) de um tenant já provisionado -- item 3.6. */
    public void atualizarBranding(String slug, String corPrimaria, String corSecundaria, String logoUrl) {
        masterJdbcTemplate.update(
                "UPDATE tenants SET cor_primaria = ?, cor_secundaria = ?, logo_url = ? WHERE slug = ?",
                corPrimaria, corSecundaria, logoUrl, slug);
    }

    /** Upload direto de logo em PNG: guarda o binário no próprio banco master. */
    public void atualizarLogo(String slug, byte[] dados, String contentType) {
        masterJdbcTemplate.update(
                "UPDATE tenants SET logo_dados = ?, logo_content_type = ? WHERE slug = ?",
                dados, contentType, slug);
    }

    /** Remove o logo enviado por upload (a prefeitura volta a usar a URL externa, se houver, ou o padrão do produto). */
    public void removerLogo(String slug) {
        masterJdbcTemplate.update(
                "UPDATE tenants SET logo_dados = NULL, logo_content_type = NULL WHERE slug = ?", slug);
    }

    /** Busca o binário do logo enviado por upload, para servi-lo (GET /api/public/tenant/{slug}/logo). */
    public Optional<TenantLogo> buscarLogo(String slug) {
        List<TenantLogo> result = masterJdbcTemplate.query(
                "SELECT logo_dados, logo_content_type FROM tenants WHERE slug = ? AND logo_dados IS NOT NULL",
                (rs, rowNum) -> new TenantLogo(rs.getBytes("logo_dados"), rs.getString("logo_content_type")),
                slug
        );
        return result.stream().findFirst();
    }

    public void insert(TenantRecord tenant) {
        masterJdbcTemplate.update(
                """
                INSERT INTO tenants (slug, nome_municipio, db_host, db_port, db_name, db_user, db_password,
                                      cor_primaria, cor_secundaria, logo_url, ativo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true)
                """,
                tenant.slug(), tenant.nomeMunicipio(), tenant.dbHost(), tenant.dbPort(), tenant.dbName(),
                tenant.dbUser(), tenant.dbPassword(), tenant.corPrimaria(), tenant.corSecundaria(), tenant.logoUrl()
        );
    }

    /**
     * Remove o registro do tenant do master -- usado como compensação quando o
     * provisionamento falha DEPOIS do insert (ex.: migração Flyway ou criação do
     * admin inicial deram erro), para o slug não ficar "travado" impedindo uma
     * nova tentativa (ver {@link br.com.filasaude.service.SuperadminTenantService#provisionar}).
     * Não apaga o banco físico da prefeitura (pode já ter dados parciais e
     * DROP DATABASE é destrutivo demais para um rollback automático) -- ele
     * fica órfão, mas inofensivo, e é reaproveitado (CREATE DATABASE IF NOT
     * EXISTS) numa nova tentativa de provisionamento com o mesmo slug.
     */
    public void deletarPorSlug(String slug) {
        masterJdbcTemplate.update("DELETE FROM tenants WHERE slug = ?", slug);
    }

    private TenantRecord mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new TenantRecord(
                rs.getLong("id"),
                rs.getString("slug"),
                rs.getString("nome_municipio"),
                rs.getString("db_host"),
                rs.getInt("db_port"),
                rs.getString("db_name"),
                rs.getString("db_user"),
                rs.getString("db_password"),
                rs.getString("cor_primaria"),
                rs.getString("cor_secundaria"),
                rs.getString("logo_url"),
                rs.getBoolean("logo_upload"),
                rs.getBoolean("ativo")
        );
    }
}
