package br.com.filasaude.dto.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Formato (só o que interessa aqui) do payload que a Meta envia ao webhook
 * do WhatsApp Business Platform -- ver WhatsappWebhookController. O evento
 * real traz muito mais campos (contacts, timestamps, profile, etc.);
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} em cada nível evita que
 * qualquer um deles quebre o parsing, já que só extraímos phone_number_id +
 * o clique de botão.
 *
 * Referência: https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks/payload-examples
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsappWebhookPayload(
        String object,
        List<Entry> entry
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(List<Change> changes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Change(Value value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            Metadata metadata,
            List<IncomingMessage> messages
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
            @JsonProperty("phone_number_id") String phoneNumberId
    ) {
    }

    /**
     * Uma mensagem recebida. Só nos interessa {@code type == "button"}
     * (resposta a um botão de Quick Reply de um template já enviado -- ver
     * NotificacaoWhatsappService) -- outros tipos (texto livre, imagem,
     * status de entrega/leitura em "statuses", etc.) são ignorados.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IncomingMessage(
            String from,
            String type,
            ButtonReply button
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ButtonReply(
            String payload,
            String text
    ) {
    }
}
