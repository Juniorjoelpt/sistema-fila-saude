package br.com.filasaude.service;

import br.com.filasaude.dto.publico.ConfirmacaoPresencaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processa UM clique de botão (confirmar/cancelar presença) do webhook do
 * WhatsApp, já com o {@code TenantContext} resolvido para o tenant dono do
 * número que recebeu o evento (ver WhatsappWebhookController). Bean à parte
 * pelo mesmo motivo de sempre neste projeto: o {@code @Transactional} só
 * tem efeito chamado de fora do bean que o declara.
 *
 * Reaproveita {@link ProtocoloConfirmacaoService} -- a mesma ação que o
 * paciente faria clicando no link do e-mail/lembrete -- em vez de duplicar a
 * lógica de confirmar/cancelar por token.
 */
@Service
public class WhatsappWebhookTenantProcessor {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWebhookTenantProcessor.class);

    private final ProtocoloConfirmacaoService protocoloConfirmacaoService;
    private final WhatsappCredenciaisService whatsappCredenciaisService;
    private final NotificacaoWhatsappService notificacaoWhatsappService;

    public WhatsappWebhookTenantProcessor(ProtocoloConfirmacaoService protocoloConfirmacaoService,
                                           WhatsappCredenciaisService whatsappCredenciaisService,
                                           NotificacaoWhatsappService notificacaoWhatsappService) {
        this.protocoloConfirmacaoService = protocoloConfirmacaoService;
        this.whatsappCredenciaisService = whatsappCredenciaisService;
        this.notificacaoWhatsappService = notificacaoWhatsappService;
    }

    /**
     * @param acao      {@link NotificacaoWhatsappService#PAYLOAD_CONFIRMAR} ou
     *                  {@link NotificacaoWhatsappService#PAYLOAD_CANCELAR}
     * @param token     confirmacaoToken do protocolo (extraído do payload do botão)
     * @param telefoneRemetente número que clicou, no formato que a Meta já manda
     *                  (dígitos com código de país) -- usado só para a
     *                  mensagem de confirmação de volta, não para localizar
     *                  o protocolo (isso é feito só pelo token).
     */
    @Transactional
    public void processarClique(String acao, String token, String telefoneRemetente) {
        ConfirmacaoPresencaResponse resposta = switch (acao) {
            case NotificacaoWhatsappService.PAYLOAD_CONFIRMAR -> protocoloConfirmacaoService.confirmar(token);
            case NotificacaoWhatsappService.PAYLOAD_CANCELAR -> protocoloConfirmacaoService.cancelar(token);
            default -> {
                log.warn("Ação desconhecida recebida no webhook do WhatsApp: '{}'", acao);
                yield null;
            }
        };

        if (resposta == null) {
            return;
        }

        log.info("Protocolo {} atualizado via WhatsApp: presença {}", resposta.numeroProtocolo(), resposta.presencaConfirmacao());

        whatsappCredenciaisService.ativas().ifPresent(credenciais ->
                notificacaoWhatsappService.enviarMensagemTexto(
                        credenciais.phoneNumberId(), credenciais.accessToken(), telefoneRemetente,
                        mensagemConfirmacao(acao, resposta)));
    }

    private String mensagemConfirmacao(String acao, ConfirmacaoPresencaResponse resposta) {
        if (NotificacaoWhatsappService.PAYLOAD_CONFIRMAR.equals(acao)) {
            return "✓ Presença confirmada para o protocolo %s. Te esperamos!".formatted(resposta.numeroProtocolo());
        }
        return "Presença cancelada para o protocolo %s. Se precisar, entre em contato com a Secretaria de Saúde para remarcar."
                .formatted(resposta.numeroProtocolo());
    }
}
