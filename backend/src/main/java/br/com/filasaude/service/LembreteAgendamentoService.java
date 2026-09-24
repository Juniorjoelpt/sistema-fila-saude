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
 * Varredura diária de lembrete de agendamento (melhoria pós-MVP sugerida
 * após o agendamento de horário real -- ver {@code HorarioAgenda}): avisa o
 * paciente, por e-mail, um dia antes do horário marcado, com um link para
 * confirmar ou cancelar a presença sem precisar logar (ver
 * {@code ProtocoloConfirmacaoController}).
 *
 * Roda uma vez por tenant, mesmo padrão do {@link SlaAlertaService}: o
 * {@link TenantContext} é setado manualmente aqui (fora de uma requisição
 * HTTP não há {@code TenantResolverFilter} para isso) e uma falha num tenant
 * não impede a varredura dos demais. O trabalho por tenant fica em
 * {@link LembreteAgendamentoTenantProcessor}, bean à parte, para que o
 * {@code @Transactional} dele seja de fato aplicado pelo proxy do Spring.
 */
@Service
public class LembreteAgendamentoService {

    private static final Logger log = LoggerFactory.getLogger(LembreteAgendamentoService.class);

    private final MasterTenantRepository masterTenantRepository;
    private final LembreteAgendamentoTenantProcessor tenantProcessor;

    public LembreteAgendamentoService(MasterTenantRepository masterTenantRepository,
                                       LembreteAgendamentoTenantProcessor tenantProcessor) {
        this.masterTenantRepository = masterTenantRepository;
        this.tenantProcessor = tenantProcessor;
    }

    /**
     * Horário padrão 8h da manhã (fuso do servidor), configurável via
     * {@code filasaude.lembrete.cron} -- ver application.yml.
     */
    @Scheduled(cron = "${filasaude.lembrete.cron:0 0 8 * * *}")
    public void executarVarreduraDiaria() {
        List<TenantRecord> tenantsAtivos = masterTenantRepository.findAllAtivos();
        log.info("Iniciando varredura de lembrete de agendamento para {} tenant(s) ativo(s)", tenantsAtivos.size());

        for (TenantRecord tenant : tenantsAtivos) {
            try {
                TenantContext.setCurrentTenant(tenant.slug());
                tenantProcessor.processarTenantCorrente(tenant.slug());
            } catch (Exception e) {
                log.error("Falha na varredura de lembrete do tenant '{}': {}", tenant.slug(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
