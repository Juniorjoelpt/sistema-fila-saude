package br.com.filasaude.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Montagem do link público de confirmação de presença -- mesmo link
 * reaproveitado pelos canais de e-mail e WhatsApp (ver
 * NotificacaoEmailService / NotificacaoWhatsappService).
 */
class ConfirmacaoLinkServiceTest {

    @Test
    void montaLinkNoPadraoDeSubdominioDeProducao() {
        ConfirmacaoLinkService service = new ConfirmacaoLinkService("https://{tenant}.filasaude.com.br");

        String link = service.montar("prefeitura-x", "tok-123");

        assertThat(link).isEqualTo("https://prefeitura-x.filasaude.com.br/confirmar-presenca?token=tok-123");
    }

    @Test
    void montaLinkNoPadraoLocalComQueryStringDeTenant() {
        ConfirmacaoLinkService service = new ConfirmacaoLinkService("http://localhost:5173/?tenant={tenant}");

        String link = service.montar("prefeitura-x", "tok-123");

        assertThat(link).isEqualTo("http://localhost:5173/confirmar-presenca?tenant=prefeitura-x&token=tok-123");
    }
}
