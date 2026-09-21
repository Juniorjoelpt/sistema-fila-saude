package br.com.filasaude.tenancy;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;

/**
 * DataSource "virtual" usado pelo Hibernate/JPA: a cada operação, resolve para o
 * datasource real do tenant corrente (ver {@link TenantContext}), delegando a
 * criação/cache dos pools ao {@link TenantDataSourceRegistry}.
 *
 * Diferente do uso "clássico" do AbstractRoutingDataSource (mapa fixo definido no
 * startup), aqui a resolução é dinâmica via {@link #determineTargetDataSource()}
 * sobrescrito, o que permite adicionar tenants novos em tempo de execução.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final TenantDataSourceRegistry registry;

    public TenantRoutingDataSource(TenantDataSourceRegistry registry) {
        this.registry = registry;
        // Mapa vazio: a resolução real acontece em determineTargetDataSource(), abaixo.
        setTargetDataSources(new HashMap<>());
        afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getCurrentTenant();
    }

    @Override
    protected DataSource determineTargetDataSource() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            throw new IllegalStateException(
                    "Nenhum tenant resolvido para a requisição atual. " +
                    "Verifique se o header X-Tenant-Id (ou subdomínio) foi enviado."
            );
        }
        return registry.getDataSource(tenant);
    }
}
