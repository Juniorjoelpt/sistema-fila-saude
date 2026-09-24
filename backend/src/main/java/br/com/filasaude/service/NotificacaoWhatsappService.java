package br.com.filasaude.service;

import br.com.filasaude.domain.Protocolo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Lembrete de agendamento por WhatsApp (canal opcional, por tenant --
 * melhoria pós-MVP sobre o lembrete por e-mail já existente). Usa o WhatsApp
 * Business Platform (Cloud API da Meta): cada prefeitura conecta sua própria
 * conta comercial na tela de Integrações (ver IntegracaoConfigService,
 * TipoIntegracao.WHATSAPP), guardando ali o Phone Number ID (campo
 * "baseUrl") e o access token (campo "token"). Enquanto não configurado ou
 * desativado, o lembrete continua saindo só por e-mail -- este canal nunca é
 * obrigatório.
 *
 * A mensagem assume um template já aprovado pela Meta com o nome/idioma
 * configurados em filasaude.whatsapp.template-lembrete -- sem essa aprovação
 * prévia, o envio falha (best-effort, só loga o erro) e não afeta o lembrete
 * por e-mail. O template precisa ter dois botões de resposta rápida (Quick
 * Reply) na ordem "Confirmar presença" / "Não vou comparecer" -- ver
 * {@link #PAYLOAD_CONFIRMAR}/{@link #PAYLOAD_CANCELAR} e o texto sugerido no
 * Javadoc de {@code WhatsappWebhookController}, que processa o clique.
 */
@Service
public class NotificacaoWhatsappService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoWhatsappService.class);
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Prefixo do payload devolvido pela Meta no clique de cada botão do
     * template de lembrete (ver {@link #montarPayload}), seguido de
     * ":<confirmacaoToken>" -- é assim que o webhook (WhatsappWebhookController
     * /WhatsappWebhookTenantProcessor) identifica tanto a ação escolhida
     * quanto o protocolo, sem precisar de nenhum estado adicional.
     */
    public static final String PAYLOAD_CONFIRMAR = "CONFIRMAR";
    public static final String PAYLOAD_CANCELAR = "CANCELAR";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ConfirmacaoLinkService confirmacaoLinkService;
    private final String graphApiVersion;
    private final String templateLembrete;
    private final String idiomaTemplate;

    public NotificacaoWhatsappService(ConfirmacaoLinkService confirmacaoLinkService,
                                       @Value("${filasaude.whatsapp.graph-api-version:v21.0}") String graphApiVersion,
                                       @Value("${filasaude.whatsapp.template-lembrete:lembrete_agendamento}") String templateLembrete,
                                       @Value("${filasaude.whatsapp.idioma-template:pt_BR}") String idiomaTemplate) {
        this.confirmacaoLinkService = confirmacaoLinkService;
        this.graphApiVersion = graphApiVersion;
        this.templateLembrete = templateLembrete;
        this.idiomaTemplate = idiomaTemplate;
    }

    /**
     * As credenciais (phoneNumberId/accessToken) são recebidas como
     * parâmetro, já lidas do IntegracaoConfig do tenant pelo chamador
     * (LembreteAgendamentoTenantProcessor) -- este método roda em outra
     * thread (@Async) e não tem acesso ao TenantContext (ThreadLocal) nem a
     * uma sessão do Hibernate para buscar essas configurações sozinho. Pelo
     * mesmo motivo, o protocolo recebido aqui já vem com paciente/
     * procedimento/horário/unidade inicializados (ver
     * ProtocoloRepository#findParaLembreteAgendamento).
     */
    @Async
    public void notificarLembreteAgendamento(Protocolo protocolo, String tenantSlug,
                                              String phoneNumberId, String accessToken) {
        String telefone = normalizarTelefone(protocolo.getPaciente().getTelefone());
        if (telefone == null) {
            log.debug("Paciente do protocolo {} não possui telefone válido cadastrado; lembrete por WhatsApp não enviado",
                    protocolo.getNumeroProtocolo());
            return;
        }

        try {
            String link = confirmacaoLinkService.montar(tenantSlug, protocolo.getConfirmacaoToken());
            String url = "https://graph.facebook.com/%s/%s/messages".formatted(graphApiVersion, phoneNumberId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            restTemplate.postForEntity(url, new HttpEntity<>(montarPayload(protocolo, telefone, link), headers), String.class);
            log.info("Lembrete de agendamento por WhatsApp enviado para o protocolo {}", protocolo.getNumeroProtocolo());
        } catch (Exception e) {
            log.warn("Falha ao enviar lembrete de agendamento por WhatsApp do protocolo {}: {}",
                    protocolo.getNumeroProtocolo(), e.getMessage());
        }
    }

    private Map<String, Object> montarPayload(Protocolo protocolo, String telefone, String link) {
        String unidade = protocolo.getHorarioAgendado() != null && protocolo.getHorarioAgendado().getUnidadeSaude() != null
                ? protocolo.getHorarioAgendado().getUnidadeSaude().getNome()
                : "a unidade informada";
        String hora = protocolo.getHorarioAgendado() != null
                ? protocolo.getHorarioAgendado().getHoraInicio().format(FORMATO_HORA)
                : "";
        String dataHora = protocolo.getDataPrevista() != null
                ? protocolo.getDataPrevista().format(FORMATO_DATA) + (hora.isBlank() ? "" : " às " + hora)
                : "a data marcada";

        // Corpo do template com 4 variáveis posicionais ({{1}}..{{4}}): nome,
        // procedimento, data/hora e unidade+link. O texto exato do template
        // (aprovado previamente na Meta) não é definido aqui -- só os valores
        // que preenchem as variáveis na ordem esperada.
        List<Map<String, Object>> parametros = List.of(
                parametroTexto(protocolo.getPaciente().getNome()),
                parametroTexto(protocolo.getProcedimento().getNome()),
                parametroTexto(dataHora),
                parametroTexto(unidade + " — " + link)
        );

        // Os dois botões de resposta rápida do template levam o token do
        // protocolo embutido no payload (ex.: "CONFIRMAR:<token>") -- é assim
        // que o webhook sabe, ao receber o clique de volta, tanto qual ação
        // foi escolhida quanto qual protocolo confirmar/cancelar.
        String token = protocolo.getConfirmacaoToken();
        List<Map<String, Object>> componentes = List.of(
                Map.of("type", "body", "parameters", parametros),
                componenteBotao(0, PAYLOAD_CONFIRMAR + ":" + token),
                componenteBotao(1, PAYLOAD_CANCELAR + ":" + token)
        );

        return Map.of(
                "messaging_product", "whatsapp",
                "to", telefone,
                "type", "template",
                "template", Map.of(
                        "name", templateLembrete,
                        "language", Map.of("code", idiomaTemplate),
                        "components", componentes
                )
        );
    }

    private Map<String, Object> componenteBotao(int indice, String payload) {
        return Map.of(
                "type", "button",
                "sub_type", "quick_reply",
                "index", String.valueOf(indice),
                "parameters", List.of(Map.of("type", "payload", "payload", payload))
        );
    }

    private Map<String, Object> parametroTexto(String texto) {
        return Map.of("type", "text", "text", texto);
    }

    /**
     * Mensagem de texto livre em resposta ao clique num botão (ver
     * WhatsappWebhookTenantProcessor) -- confirmação/cancelamento
     * reconhecidos. Permitida e gratuita dentro da janela de 24h aberta pelo
     * próprio paciente ao responder o lembrete (categoria "service", não
     * precisa de template aprovado).
     */
    @Async
    public void enviarMensagemTexto(String phoneNumberId, String accessToken, String telefoneDestino, String texto) {
        try {
            String url = "https://graph.facebook.com/%s/%s/messages".formatted(graphApiVersion, phoneNumberId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "to", telefoneDestino,
                    "type", "text",
                    "text", Map.of("body", texto)
            );
            restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        } catch (Exception e) {
            log.warn("Falha ao enviar mensagem de confirmação por WhatsApp para {}: {}", telefoneDestino, e.getMessage());
        }
    }

    /**
     * Normaliza para o formato exigido pela Cloud API (código do país + DDD +
     * número, só dígitos, sem "+"). Assume Brasil (55) quando o número não
     * vem com código de país -- consistente com o restante do produto, hoje
     * voltado a prefeituras brasileiras.
     */
    private String normalizarTelefone(String telefoneBruto) {
        if (telefoneBruto == null || telefoneBruto.isBlank()) {
            return null;
        }
        String digitos = telefoneBruto.replaceAll("\\D", "");
        if (digitos.isBlank()) {
            return null;
        }
        if (digitos.length() == 10 || digitos.length() == 11) {
            return "55" + digitos;
        }
        return digitos;
    }
}
