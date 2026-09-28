package br.com.filasaude.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validação da assinatura HMAC-SHA256 do webhook do WhatsApp (header
 * "X-Hub-Signature-256") -- computa a assinatura real com o mesmo
 * algoritmo, sem precisar de mocks.
 */
class WhatsappAssinaturaValidatorTest {

    private static final String APP_SECRET = "segredo-de-teste-123";

    private String assinar(String segredo, byte[] corpo) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(corpo);
        return "sha256=" + HexFormat.of().formatHex(digest);
    }

    @Test
    void aceitaAssinaturaCorretamenteCalculada() throws Exception {
        WhatsappAssinaturaValidator validator = new WhatsappAssinaturaValidator(APP_SECRET);
        byte[] corpo = "{\"evento\":\"teste\"}".getBytes(StandardCharsets.UTF_8);
        String assinatura = assinar(APP_SECRET, corpo);

        assertThat(validator.valida(corpo, assinatura)).isTrue();
    }

    @Test
    void rejeitaAssinaturaComSegredoErrado() throws Exception {
        WhatsappAssinaturaValidator validator = new WhatsappAssinaturaValidator(APP_SECRET);
        byte[] corpo = "{\"evento\":\"teste\"}".getBytes(StandardCharsets.UTF_8);
        String assinaturaErrada = assinar("segredo-errado", corpo);

        assertThat(validator.valida(corpo, assinaturaErrada)).isFalse();
    }

    @Test
    void rejeitaQuandoCorpoFoiAlterado() throws Exception {
        WhatsappAssinaturaValidator validator = new WhatsappAssinaturaValidator(APP_SECRET);
        byte[] corpoOriginal = "{\"evento\":\"original\"}".getBytes(StandardCharsets.UTF_8);
        String assinatura = assinar(APP_SECRET, corpoOriginal);
        byte[] corpoAdulterado = "{\"evento\":\"adulterado\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.valida(corpoAdulterado, assinatura)).isFalse();
    }

    @Test
    void rejeitaHeaderSemPrefixoEsperado() {
        WhatsappAssinaturaValidator validator = new WhatsappAssinaturaValidator(APP_SECRET);
        byte[] corpo = "{}".getBytes(StandardCharsets.UTF_8);

        assertThat(validator.valida(corpo, "abc123")).isFalse();
        assertThat(validator.valida(corpo, null)).isFalse();
    }

    @Test
    void deixaPassarQuandoAppSecretNaoConfigurado() {
        WhatsappAssinaturaValidator validator = new WhatsappAssinaturaValidator("");
        byte[] corpo = "{}".getBytes(StandardCharsets.UTF_8);

        // Sem App Secret (ambiente local sem conta Meta ainda) o validador não
        // trava o desenvolvimento -- deixa passar mesmo sem assinatura válida.
        assertThat(validator.valida(corpo, "sha256=qualquercoisa")).isTrue();
        assertThat(validator.valida(corpo, null)).isTrue();
    }
}
