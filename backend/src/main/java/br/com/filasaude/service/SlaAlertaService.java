package br.com.filasaude.service;

import br.com.filasaude.tenancy.MasterTenantRepository;
import br.com.filasaude.tenancy.TenantContext;
import br.com.filasaude.tenancy.TenantRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Varredura diária de SLA vencido (item de melhoria pós-MVP: hoje o prazo
 * "Atrasado" só aparecia como badge na tela da fila -- ver
 * {@code nivelPrazo}/{@code PrazoBadge} no frontend -- sem nenhum alerta
 * proativo a quem opera a fila).
 *
 * Roda uma vez por tenant (prefeitura), já que cada uma tem seu próprio banco
 * (ver {@link br.com.filasaude.tenancy}): o {@link TenantContext} é setado
 * manualmente aqui porque, fora de uma requisição HTTP, não há o
 * {@code TenantResolverFilter} para fazer isso. Um tenant com falha (ex.:
 * banco fora do ar) não impede a varredura dos demais. O trabalho por tenant
 * fica em {@link SlaAlertaTenantProcessor}, um bean à parte, para que o
 * {@code @Transactional} dele seja de fato aplicado pelo proxy do Spring
 * (chamada externa a este service, não self-invocation).
 */
@Service
public class SlaAlertaService {

    private static final Logger log = LoggerFactory.getLogger(SlaAlertaService.class);

    private final MasterTenantRepository masterTenantRepository;
    private final SlaAlertaTenantProcessor tenantProcessor;

    public SlaAlertaService(MasterTenantRepository masterTenantRepository,
                             SlaAlertaTenantProcessor tenantProcessor) {
        this.masterTenantRepository = masterTenantRepository;
        this.tenantProcessor = tenantProcessor;
    }

    /**
     * Horário padrão 7h da manhã (fuso do servidor), configurável via
     * {@code filasaude.sla.cron} -- ver application.yml.
     */
    @Scheduled(cron = "${filasaude.sla.cron:0 0 7 * * *}")
    public void executarVarreduraDiaria() {
        List<TenantRecord> tenantsAtivos = masterTenantRepository.findAllAtivos();
        log.info("Iniciando varredura de SLA para {} tenant(s) ativo(s)", tenantsAtivos.size());

        for (TenantRecord tenant : tenantsAtivos) {
            try {
                TenantContext.setCurrentTenant(tenant.slug());
                tenantProcessor.processarTenantCorrente();
            } catch (Exception e) {
                log.error("Falha na varredura de SLA do tenant '{}': {}", tenant.slug(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
