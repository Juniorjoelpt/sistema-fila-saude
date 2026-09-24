package br.com.filasaude.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Monta o link público de confirmação de presença (ver
 * ProtocoloConfirmacaoController) a partir de
 * {@code filasaude.frontend.url-pattern}. Extraído de
 * {@code NotificacaoEmailService} para ser reaproveitado também pelo canal
 * WhatsApp ({@code NotificacaoWhatsappService}) -- o link é o mesmo,
 * independente do canal que o entrega.
 *
 * Suporta tanto o formato de produção (subdomínio, ex.:
 * "https://{tenant}.filasaude.com.br") quanto uma eventual sobrescrita local
 * baseada em query string (ex.: "http://localhost:5173/?tenant={tenant}"),
 * decidindo o separador da query de acordo com o que já houver no padrão.
 */
@Component
public class ConfirmacaoLinkService {

    private final String frontendUrlPattern;

    public ConfirmacaoLinkService(
            @Value("${filasaude.frontend.url-pattern:https://{tenant}.filasaude.com.br}") String frontendUrlPattern) {
        this.frontendUrlPattern = frontendUrlPattern;
    }

    public String montar(String tenantSlug, String token) {
        String base = frontendUrlPattern.replace("{tenant}", tenantSlug);
        String caminho = "confirmar-presenca";

        if (base.contains("?")) {
            // Padrão local com query string de tenant: acrescenta o caminho antes
            // da query para não quebrar o roteamento client-side (ex.: /confirmar-presenca?tenant=x&token=y).
            int idx = base.indexOf('?');
            return base.substring(0, idx) + caminho + base.substring(idx) + "&token=" + token;
        }
        return base + "/" + caminho + "?token=" + token;
    }
}
