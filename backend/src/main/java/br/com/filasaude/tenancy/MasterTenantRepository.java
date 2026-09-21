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
 */
@Repository
public class MasterTenantRepository {

    private final JdbcTemplate masterJdbcTemplate;

    public MasterTenantRepository(@Qualifier("masterDataSource") DataSource masterDataSource) {
        this.masterJdbcTemplate = new JdbcTemplate(masterDataSource);
    }

    public List<TenantRecord> findAllAtivos() {
        return masterJdbcTemplate.query(
                """
                SELECT id, slug, nome_municipio, db_host, db_port, db_name, db_user, db_password,
                       cor_primaria, cor_secundaria, logo_url, ativo
                FROM tenants WHERE ativo = true
                """,
                this::mapRow
        );
    }

    public Optional<TenantRecord> findBySlug(String slug) {
        List<TenantRecord> result = masterJdbcTemplate.query(
                """
                SELECT id, slug, nome_municipio, db_host, db_port, db_name, db_user, db_password,
                       cor_primaria, cor_secundaria, logo_url, ativo
                FROM tenants WHERE slug = ? AND ativo = true
                """,
                this::mapRow,
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
                rs.getBoolean("ativo")
        );
    }
}
