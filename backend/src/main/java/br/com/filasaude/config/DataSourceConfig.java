package br.com.filasaude.config;

import br.com.filasaude.tenancy.TenantDataSourceRegistry;
import br.com.filasaude.tenancy.TenantRoutingDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Dois datasources coexistem na aplicação:
 *
 *  - "masterDataSource": banco fixo, único, com o cadastro de prefeituras (tenants).
 *    Usado apenas pelo MasterTenantRepository, que constrói seu próprio JdbcTemplate
 *    internamente a partir deste bean (ver o comentário em MasterTenantRepository
 *    sobre por que ele NÃO é exposto aqui como um @Bean JdbcTemplate — isso causava
 *    um ciclo de dependência com o datasource @Primary abaixo).
 *
 *  - "tenantRoutingDataSource": datasource "virtual" usado pelo JPA/Hibernate para
 *    todas as entidades de negócio (Paciente, ItemFila, etc.), que roteia para o
 *    banco físico do tenant corrente a cada operação.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "filasaude.master-datasource")
    public HikariConfig masterHikariConfig() {
        return new HikariConfig();
    }

    @Bean(name = "masterDataSource")
    public DataSource masterDataSource(@Qualifier("masterHikariConfig") HikariConfig config) {
        return new HikariDataSource(config);
    }

    @Bean
    @Primary
    public DataSource tenantRoutingDataSource(TenantDataSourceRegistry registry) {
        return new TenantRoutingDataSource(registry);
    }
}
