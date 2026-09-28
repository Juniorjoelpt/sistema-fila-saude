package br.com.filasaude.service;

import br.com.filasaude.dto.whatsapp.WhatsappWebhookPayload;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.tenancy.MasterWhatsappNumeroRepository;
import br.com.filasaude.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orquestração de um evento recebido do webhook único do WhatsApp: resolve o
 * tenant a partir do phone_number_id (mapeamento no banco master) antes de
 * processar, e isola qualquer falha -- nunca deve propagar para o
 * controller, que sempre responde 200 à Meta.
 */
@ExtendWith(MockitoExtension.class)
class WhatsappWebhookServiceTest {

    @Mock private MasterWhatsappNumeroRepository masterWhatsappNumeroRepository;
    @Mock private WhatsappWebhookTenantProcessor tenantProcessor;

    private WhatsappWebhookService service() {
        return new WhatsappWebhookService(masterWhatsappNumeroRepository, tenantProcessor);
    }

    @AfterEach
    void limpaTenantContext() {
        // Proteção contra vazamento de TenantContext entre testes, já que ele é
        // um ThreadLocal real (não mockado) e os testes rodam na mesma thread.
        TenantContext.clear();
    }

    private WhatsappWebhookPayload payloadComBotao(String phoneNumberId, String payloadBotao, String from) {
        WhatsappWebhookPayload.ButtonReply botao = new WhatsappWebhookPayload.ButtonReply(payloadBotao, "texto");
        WhatsappWebhookPayload.IncomingMessage mensagem = new WhatsappWebhookPayload.IncomingMessage(from, "button", botao);
        WhatsappWebhookPayload.Metadata metadata = new WhatsappWebhookPayload.Metadata(phoneNumberId);
        WhatsappWebhookPayload.Value valor = new WhatsappWebhookPayload.Value(metadata, List.of(mensagem));
        WhatsappWebhookPayload.Change change = new WhatsappWebhookPayload.Change(valor);
        WhatsappWebhookPayload.Entry entry = new WhatsappWebhookPayload.Entry(List.of(change));
        return new WhatsappWebhookPayload("whatsapp_business_account", List.of(entry));
    }

    @Test
    void ignoraPayloadNuloComSeguranca() {
        service().processar(null);
        verify(tenantProcessor, never()).processarClique(anyString(), anyString(), anyString());
    }

    @Test
    void ignoraPayloadSemEntry() {
        service().processar(new WhatsappWebhookPayload("whatsapp_business_account", null));
        verify(tenantProcessor, never()).processarClique(anyString(), anyString(), anyString());
    }

    @Test
    void ignoraMensagemQueNaoEhCliqueDeBotao() {
        WhatsappWebhookPayload.IncomingMessage mensagemTexto = new WhatsappWebhookPayload.IncomingMessage("5586999999999", "text", null);
        WhatsappWebhookPayload.Metadata metadata = new WhatsappWebhookPayload.Metadata("123456");
        WhatsappWebhookPayload.Value valor = new WhatsappWebhookPayload.Value(metadata, List.of(mensagemTexto));
        WhatsappWebhookPayload.Change change = new WhatsappWebhookPayload.Change(valor);
        WhatsappWebhookPayload.Entry entry = new WhatsappWebhookPayload.Entry(List.of(change));
        WhatsappWebhookPayload payload = new WhatsappWebhookPayload("whatsapp_business_account", List.of(entry));

        service().processar(payload);

        verify(tenantProcessor, never()).processarClique(anyString(), anyString(), anyString());
        verify(masterWhatsappNumeroRepository, never()).buscarTenantSlug(anyString());
    }

    @Test
    void ignoraEventoDePhoneNumberIdSemTenantMapeado() {
        WhatsappWebhookPayload payload = payloadComBotao("123456", "CONFIRMAR:token-abc", "5586999999999");
        when(masterWhatsappNumeroRepository.buscarTenantSlug("123456")).thenReturn(Optional.empty());

        service().processar(payload);

        verify(tenantProcessor, never()).processarClique(anyString(), anyString(), anyString());
    }

    @Test
    void ignoraPayloadDeBotaoEmFormatoInesperadoSemDoisPontos() {
        WhatsappWebhookPayload payload = payloadComBotao("123456", "payload-sem-separador", "5586999999999");

        service().processar(payload);

        verify(tenantProcessor, never()).processarClique(anyString(), anyString(), anyString());
        verify(masterWhatsappNumeroRepository, never()).buscarTenantSlug(anyString());
    }

    @Test
    void resolveTenantESetaTenantContextAntesDeProcessarClique() {
        WhatsappWebhookPayload payload = payloadComBotao("123456", "CONFIRMAR:token-abc", "5586999999999");
        when(masterWhatsappNumeroRepository.buscarTenantSlug("123456")).thenReturn(Optional.of("prefeitura-x"));

        service().processar(payload);

        verify(tenantProcessor).processarClique("CONFIRMAR", "token-abc", "5586999999999");
        // TenantContext deve ter sido limpo ao final, mesmo em caso de sucesso.
        assertNullTenantContext();
    }

    @Test
    void isolaExcecaoDeTokenInvalidoSemPropagarELimpaTenantContext() {
        WhatsappWebhookPayload payload = payloadComBotao("123456", "CANCELAR:token-invalido", "5586999999999");
        when(masterWhatsappNumeroRepository.buscarTenantSlug("123456")).thenReturn(Optional.of("prefeitura-x"));
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Link de confirmação inválido ou expirado"))
                .when(tenantProcessor).processarClique("CANCELAR", "token-invalido", "5586999999999");

        service().processar(payload); // não deve lançar

        assertNullTenantContext();
    }

    @Test
    void isolaQualquerOutraExcecaoSemPropagarELimpaTenantContext() {
        WhatsappWebhookPayload payload = payloadComBotao("123456", "CONFIRMAR:token-abc", "5586999999999");
        when(masterWhatsappNumeroRepository.buscarTenantSlug("123456")).thenReturn(Optional.of("prefeitura-x"));
        org.mockito.Mockito.doThrow(new RuntimeException("falha inesperada"))
                .when(tenantProcessor).processarClique("CONFIRMAR", "token-abc", "5586999999999");

        service().processar(payload); // não deve lançar

        assertNullTenantContext();
    }

    @Test
    void processaMultiplosEventosDeEntriesEChangesDiferentes() {
        WhatsappWebhookPayload payload1 = payloadComBotao("123456", "CONFIRMAR:token-1", "5586900000001");
        WhatsappWebhookPayload payload2 = payloadComBotao("789012", "CANCELAR:token-2", "5586900000002");
        WhatsappWebhookPayload combinado = new WhatsappWebhookPayload("whatsapp_business_account",
                List.of(payload1.entry().get(0), payload2.entry().get(0)));
        when(masterWhatsappNumeroRepository.buscarTenantSlug("123456")).thenReturn(Optional.of("prefeitura-x"));
        when(masterWhatsappNumeroRepository.buscarTenantSlug("789012")).thenReturn(Optional.of("prefeitura-y"));

        service().processar(combinado);

        verify(tenantProcessor, times(1)).processarClique("CONFIRMAR", "token-1", "5586900000001");
        verify(tenantProcessor, times(1)).processarClique("CANCELAR", "token-2", "5586900000002");
    }

    private void assertNullTenantContext() {
        org.assertj.core.api.Assertions.assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}
