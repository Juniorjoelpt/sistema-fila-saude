package br.com.filasaude.controller;

import br.com.filasaude.dto.whatsapp.WhatsappWebhookPayload;
import br.com.filasaude.security.WhatsappAssinaturaValidator;
import br.com.filasaude.service.WhatsappWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook ÚNICO (compartilhado por todos os tenants) do WhatsApp Business
 * Platform -- registrado uma vez no app da Meta usado pela MS Soluções,
 * recebendo eventos de todas as contas comerciais conectadas pelas
 * prefeituras (ver TipoIntegracao.WHATSAPP / tela de Integrações). Por isso
 * NÃO passa pelo TenantResolverFilter normal (a Meta não manda X-Tenant-Id
 * nem bate num subdomínio de prefeitura) -- a resolução do tenant acontece
 * "na mão", dentro do corpo do evento, em {@link WhatsappWebhookService}
 * (via {@code phone_number_id} -> {@code MasterWhatsappNumeroRepository}).
 *
 * Rota pública ({@code permitAll} em SecurityConfig, obrigatório -- a Meta
 * não tem como se autenticar de outra forma), protegida pela verificação de
 * assinatura HMAC (ver {@link WhatsappAssinaturaValidator}) no POST e pelo
 * "hub.verify_token" no GET de verificação inicial (fluxo padrão da Meta:
 * https://developers.facebook.com/docs/graph-api/webhooks/getting-started).
 *
 * TEMPLATE ESPERADO (a submeter na Meta antes de ativar, categoria UTILITY):
 * corpo com 4 variáveis (nome, procedimento, data/hora, unidade+link -- ver
 * NotificacaoWhatsappService#montarPayload) e dois botões de resposta rápida,
 * nesta ordem: "Confirmar presença" (índice 0) / "Não vou comparecer"
 * (índice 1) -- os índices têm que bater com {@code componenteBotao(0, ...)}
 * /{@code componenteBotao(1, ...)} no envio.
 */
@RestController
@RequestMapping("/api/public/whatsapp/webhook")
public class WhatsappWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWebhookController.class);
    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

    private final WhatsappWebhookService whatsappWebhookService;
    private final WhatsappAssinaturaValidator assinaturaValidator;
    private final ObjectMapper objectMapper;
    private final String verifyToken;
    private final HttpServletRequest request;

    public WhatsappWebhookController(WhatsappWebhookService whatsappWebhookService,
                                      WhatsappAssinaturaValidator assinaturaValidator,
                                      ObjectMapper objectMapper,
                                      @Value("${filasaude.whatsapp.webhook-verify-token:}") String verifyToken,
                                      HttpServletRequest request) {
        this.whatsappWebhookService = whatsappWebhookService;
        this.assinaturaValidator = assinaturaValidator;
        this.objectMapper = objectMapper;
        this.verifyToken = verifyToken;
        this.request = request;
    }

    /**
     * Verificação inicial exigida pela Meta ao registrar a URL do webhook:
     * ecoa "hub.challenge" de volta se "hub.verify_token" bater com
     * {@code filasaude.whatsapp.webhook-verify-token}.
     */
    @GetMapping
    public ResponseEntity<String> verificar(@RequestParam("hub.mode") String modo,
                                             @RequestParam("hub.verify_token") String tokenRecebido,
                                             @RequestParam("hub.challenge") String challenge) {
        if (!"subscribe".equals(modo) || verifyToken.isBlank() || !verifyToken.equals(tokenRecebido)) {
            log.warn("Verificação do webhook do WhatsApp rejeitada (modo='{}')", modo);
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(challenge);
    }

    /**
     * Evento real (mensagem recebida, status de entrega, etc.). O corpo é
     * lido como bytes crus (não como objeto já desserializado) porque a
     * assinatura HMAC é calculada sobre o payload exato enviado -- qualquer
     * reserialização mudaria os bytes e invalidaria a verificação.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> receber(@RequestBody byte[] corpoBruto) {
        String assinatura = request.getHeader(SIGNATURE_HEADER);
        if (!assinaturaValidator.valida(corpoBruto, assinatura)) {
            log.warn("Assinatura inválida no webhook do WhatsApp -- evento rejeitado");
            return ResponseEntity.status(401).build();
        }

        try {
            WhatsappWebhookPayload payload = objectMapper.readValue(corpoBruto, WhatsappWebhookPayload.class);
            whatsappWebhookService.processar(payload);
        } catch (Exception e) {
            // Sempre responde 200 (recomendação da Meta, evita reenvio em
            // backoff por erro nosso) -- qualquer falha já foi logada dentro
            // de WhatsappWebhookService, isolada por evento.
            log.error("Falha ao processar payload do webhook do WhatsApp: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
