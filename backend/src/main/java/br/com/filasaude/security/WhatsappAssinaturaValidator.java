package br.com.filasaude.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;

/**
 * Valida a assinatura HMAC-SHA256 que a Meta envia no header
 * "X-Hub-Signature-256" de todo evento do webhook do WhatsApp -- prova de
 * que a requisição realmente veio da Meta (e não de qualquer um batendo
 * nesta URL pública, já que o webhook precisa ser {@code permitAll()} --
 * ver SecurityConfig). Calculada com o App Secret do app da Meta usado para
 * a integração (único, compartilhado por todos os tenants -- diferente do
 * access token, que é por tenant).
 */
@Component
public class WhatsappAssinaturaValidator {

    private static final Logger log = LoggerFactory.getLogger(WhatsappAssinaturaValidator.class);
    private static final String PREFIXO = "sha256=";
    private static final Set<String> PERFIS_PRODUCAO = Set.of("prod", "production", "producao");

    private final String appSecret;
    private final Environment environment;

    public WhatsappAssinaturaValidator(@Value("${filasaude.whatsapp.app-secret:}") String appSecret,
                                        Environment environment) {
        this.appSecret = appSecret;
        this.environment = environment;
    }

    /**
     * @param corpoBruto        corpo exato da requisição, em bytes (a assinatura é sobre o payload cru, não o objeto já desserializado)
     * @param assinaturaHeader  valor do header "X-Hub-Signature-256" (formato "sha256=<hex>")
     */
    public boolean valida(byte[] corpoBruto, String assinaturaHeader) {
        if (appSecret == null || appSecret.isBlank()) {
            // Bug alto de revisão corrigido: sem App Secret configurado, o
            // validador deixava QUALQUER requisição passar (fail-open) --
            // como o endpoint do webhook precisa ser público (permitAll()),
            // isso significava que qualquer um na internet podia forjar
            // eventos do WhatsApp (ex.: confirmar presença de outra pessoa)
            // simplesmente não configurando nada. Em produção isso agora
            // falha fechado (rejeita); em desenvolvimento continua avisando
            // e deixando passar, para não travar quem ainda não tem uma
            // conta da Meta configurada.
            boolean producao = Arrays.stream(environment.getActiveProfiles())
                    .map(String::toLowerCase)
                    .anyMatch(PERFIS_PRODUCAO::contains);
            if (producao) {
                log.error("filasaude.whatsapp.app-secret não configurado em produção -- rejeitando webhook do WhatsApp");
                return false;
            }
            log.warn("filasaude.whatsapp.app-secret não configurado -- assinatura do webhook não verificada (ambiente não-produção)");
            return true;
        }
        if (assinaturaHeader == null || !assinaturaHeader.startsWith(PREFIXO)) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperada = mac.doFinal(corpoBruto);
            byte[] recebida = HexFormat.of().parseHex(assinaturaHeader.substring(PREFIXO.length()));
            return MessageDigest.isEqual(esperada, recebida);
        } catch (Exception e) {
            log.warn("Falha ao validar assinatura do webhook do WhatsApp: {}", e.getMessage());
            return false;
        }
    }
}
