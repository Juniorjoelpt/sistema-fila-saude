package br.com.filasaude.service;

import br.com.filasaude.dto.whatsapp.WhatsappWebhookPayload;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.tenancy.MasterWhatsappNumeroRepository;
import br.com.filasaude.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Orquestra um evento recebido no webhook único do WhatsApp (ver
 * WhatsappWebhookController): para cada mensagem de clique de botão, resolve
 * a qual tenant o phone_number_id pertence (ver MasterWhatsappNumeroRepository)
 * e só então processa, com o {@code TenantContext} setado manualmente -- mesmo
 * padrão das varreduras agendadas (SlaAlertaService/LembreteAgendamentoService),
 * só que disparado por uma requisição HTTP em vez de um cron.
 *
 * Uma falha (evento de número desconhecido, token inválido, etc.) é isolada
 * e logada -- nunca propaga para o controller, que sempre responde 200 à
 * Meta (recomendação oficial, para evitar reenvio/backoff por erro nosso).
 */
@Service
public class WhatsappWebhookService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWebhookService.class);
    private static final String TIPO_MENSAGEM_BOTAO = "button";

    private final MasterWhatsappNumeroRepository masterWhatsappNumeroRepository;
    private final WhatsappWebhookTenantProcessor tenantProcessor;

    public WhatsappWebhookService(MasterWhatsappNumeroRepository masterWhatsappNumeroRepository,
                                   WhatsappWebhookTenantProcessor tenantProcessor) {
        this.masterWhatsappNumeroRepository = masterWhatsappNumeroRepository;
        this.tenantProcessor = tenantProcessor;
    }

    public void processar(WhatsappWebhookPayload payload) {
        if (payload == null || payload.entry() == null) {
            return;
        }

        for (WhatsappWebhookPayload.Entry entry : payload.entry()) {
            for (WhatsappWebhookPayload.Change change : safe(entry != null ? entry.changes() : null)) {
                processarChange(change);
            }
        }
    }

    private void processarChange(WhatsappWebhookPayload.Change change) {
        WhatsappWebhookPayload.Value valor = change != null ? change.value() : null;
        if (valor == null || valor.metadata() == null || valor.metadata().phoneNumberId() == null) {
            return;
        }

        String phoneNumberId = valor.metadata().phoneNumberId();
        for (WhatsappWebhookPayload.IncomingMessage mensagem : safe(valor.messages())) {
            processarMensagem(phoneNumberId, mensagem);
        }
    }

    private void processarMensagem(String phoneNumberId, WhatsappWebhookPayload.IncomingMessage mensagem) {
        if (mensagem == null || !TIPO_MENSAGEM_BOTAO.equals(mensagem.type()) || mensagem.button() == null) {
            return; // não é clique de botão do template de lembrete -- ignora (texto livre, imagem, status, etc.)
        }

        String payloadBotao = mensagem.button().payload();
        int separador = payloadBotao != null ? payloadBotao.indexOf(':') : -1;
        if (separador < 0) {
            log.warn("Payload de botão do WhatsApp em formato inesperado: '{}'", payloadBotao);
            return;
        }
        String acao = payloadBotao.substring(0, separador);
        String token = payloadBotao.substring(separador + 1);

        Optional<String> tenantSlug = masterWhatsappNumeroRepository.buscarTenantSlug(phoneNumberId);
        if (tenantSlug.isEmpty()) {
            log.warn("Evento do WhatsApp recebido para phone_number_id '{}' sem tenant mapeado -- ignorado", phoneNumberId);
            return;
        }

        try {
            TenantContext.setCurrentTenant(tenantSlug.get());
            tenantProcessor.processarClique(acao, token, mensagem.from());
        } catch (ResourceNotFoundException e) {
            log.warn("Token de confirmação inválido recebido via WhatsApp (tenant '{}'): {}", tenantSlug.get(), e.getMessage());
        } catch (Exception e) {
            log.error("Falha ao processar evento do WhatsApp (tenant '{}'): {}", tenantSlug.get(), e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    private <T> List<T> safe(List<T> lista) {
        return lista != null ? lista : List.of();
    }
}
