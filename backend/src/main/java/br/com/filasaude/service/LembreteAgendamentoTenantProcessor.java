package br.com.filasaude.service;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.PresencaConfirmacao;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Processa a varredura de lembrete de agendamento de UM tenant por vez.
 * Extraído do {@link LembreteAgendamentoService} pelo mesmo motivo do
 * {@link SlaAlertaTenantProcessor}: o {@code @Transactional} só tem efeito
 * quando chamado de fora do bean.
 */
@Service
public class LembreteAgendamentoTenantProcessor {

    private static final Logger log = LoggerFactory.getLogger(LembreteAgendamentoTenantProcessor.class);

    private final ProtocoloRepository protocoloRepository;
    private final WhatsappCredenciaisService whatsappCredenciaisService;
    private final NotificacaoEmailService notificacaoEmailService;
    private final NotificacaoWhatsappService notificacaoWhatsappService;
    private final int diasAntes;

    public LembreteAgendamentoTenantProcessor(ProtocoloRepository protocoloRepository,
                                               WhatsappCredenciaisService whatsappCredenciaisService,
                                               NotificacaoEmailService notificacaoEmailService,
                                               NotificacaoWhatsappService notificacaoWhatsappService,
                                               @Value("${filasaude.lembrete.dias-antes:1}") int diasAntes) {
        this.protocoloRepository = protocoloRepository;
        this.whatsappCredenciaisService = whatsappCredenciaisService;
        this.notificacaoEmailService = notificacaoEmailService;
        this.notificacaoWhatsappService = notificacaoWhatsappService;
        this.diasAntes = diasAntes;
    }

    /**
     * Assume que {@link TenantContext} já está setado pelo chamador (ver
     * {@link LembreteAgendamentoService#executarVarreduraDiaria()}) para o
     * tenant a ser processado nesta chamada. O slug é recebido também como
     * parâmetro explícito -- e não relido de {@link TenantContext} dentro do
     * e-mail assíncrono -- porque {@code @Async} roda em outra thread, e o
     * {@link TenantContext} é ThreadLocal (não atravessa a troca de thread).
     */
    @Transactional
    public void processarTenantCorrente(String tenantSlug) {
        LocalDate dataAlvo = LocalDate.now().plusDays(diasAntes);

        List<Protocolo> protocolos = protocoloRepository
                .findParaLembreteAgendamento(StatusProtocolo.AGENDADO, dataAlvo);

        if (protocolos.isEmpty()) {
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        for (Protocolo protocolo : protocolos) {
            if (protocolo.getConfirmacaoToken() == null) {
                protocolo.setConfirmacaoToken(UUID.randomUUID().toString());
            }
            protocolo.setPresencaConfirmacao(PresencaConfirmacao.PENDENTE);
            protocolo.setLembreteEnviadoEm(agora);
        }
        protocoloRepository.saveAll(protocolos);

        // O envio em si é assíncrono e best-effort (ver NotificacaoEmailService):
        // marcamos lembrete_enviado_em já aqui, mesmo padrão do alerta de SLA,
        // para não reenviar em caso de nova varredura antes do e-mail sair.
        protocolos.forEach(p -> notificacaoEmailService.notificarLembreteAgendamento(p, tenantSlug));

        // WhatsApp é um canal opcional, por tenant (ver TipoIntegracao.WHATSAPP
        // / tela de Integrações) -- só dispara quando a prefeitura configurou e
        // ativou sua própria conta comercial. As credenciais são lidas aqui,
        // dentro da transação, e passadas como parâmetro ao método @Async, pelo
        // mesmo motivo do e-mail: TenantContext e a sessão do Hibernate não
        // atravessam a troca de thread.
        whatsappCredenciaisService.ativas().ifPresent(credenciais ->
                protocolos.forEach(p -> notificacaoWhatsappService.notificarLembreteAgendamento(
                        p, tenantSlug, credenciais.phoneNumberId(), credenciais.accessToken())));

        log.info("Tenant '{}': {} lembrete(s) de agendamento enviado(s)", tenantSlug, protocolos.size());
    }
}
