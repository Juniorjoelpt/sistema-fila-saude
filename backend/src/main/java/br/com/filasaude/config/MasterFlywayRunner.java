package br.com.filasaude.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Migra o banco MASTER (cadastro de tenants) no startup da aplicação.
 * Diferente do schema de cada tenant (migrado sob demanda — ver TenantDataSourceRegistry),
 * o master é um único banco fixo, sempre disponível, então pode migrar direto no boot.
 */
@Component
@Order(0)
public class MasterFlywayRunner implements CommandLineRunner {

    private final DataSource masterDataSource;

    public MasterFlywayRunner(@Qualifier("masterDataSource") DataSource masterDataSource) {
        this.masterDataSource = masterDataSource;
    }

    @Override
    public void run(String... args) {
        Flyway.configure()
                .dataSource(masterDataSource)
                .locations("classpath:db/migration/master")
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
}
