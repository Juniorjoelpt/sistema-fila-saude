package br.com.filasaude.tenancy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantém em cache um HikariDataSource (pool de conexões) por tenant, criado sob demanda
 * a partir do registro em {@link MasterTenantRepository}. Cada prefeitura tem seu próprio
 * banco MySQL, então cada uma ganha aqui o seu próprio pool isolado.
 *
 * Novo tenant provisionado (fase 2 — painel de superadmin) fica disponível no próximo
 * lookup, sem precisar reiniciar a aplicação: basta chamar {@link #evict(String)} ou
 * simplesmente deixar o cache resolver na primeira requisição daquele tenant.
 */
@Component
public class TenantDataSourceRegistry {

    private static final Logger log = LoggerFactory.getLogger(TenantDataSourceRegistry.class);

    private final MasterTenantRepository masterTenantRepository;
    private final Map<String, DataSource> cache = new ConcurrentHashMap<>();

    public TenantDataSourceRegistry(MasterTenantRepository masterTenantRepository) {
        this.masterTenantRepository = masterTenantRepository;
    }

    public DataSource getDataSource(String tenantSlug) {
        return cache.computeIfAbsent(tenantSlug, this::buildDataSource);
    }

    public boolean tenantExists(String tenantSlug) {
        if (cache.containsKey(tenantSlug)) {
            return true;
        }
        return masterTenantRepository.findBySlug(tenantSlug).isPresent();
    }

    public void evict(String tenantSlug) {
        DataSource removed = cache.remove(tenantSlug);
        if (removed instanceof HikariDataSource hikari) {
            hikari.close();
        }
    }

    private DataSource buildDataSource(String tenantSlug) {
        TenantRecord tenant = masterTenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new TenantNotFoundException(tenantSlug));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(tenant.jdbcUrl());
        config.setUsername(tenant.dbUser());
        config.setPassword(tenant.dbPassword());
        config.setPoolName("tenant-" + tenantSlug);
        config.setMaximumPoolSize(8);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        HikariDataSource dataSource = new HikariDataSource(config);

        // Migração sob demanda: no boot da aplicação ainda não há tenant resolvido
        // (ver TenantContext), então o schema de cada tenant é aplicado/atualizado
        // no primeiro acesso àquele tenant, não no startup do app.
        log.info("Aplicando migrações Flyway para o tenant '{}'", tenantSlug);
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .load()
                .migrate();

        return dataSource;
    }
}
