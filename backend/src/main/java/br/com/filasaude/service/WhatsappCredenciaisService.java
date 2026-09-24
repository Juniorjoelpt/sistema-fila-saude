package br.com.filasaude.service;

import br.com.filasaude.domain.IntegracaoConfig;
import br.com.filasaude.domain.enums.TipoIntegracao;
import br.com.filasaude.repository.IntegracaoConfigRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Credenciais da conta WhatsApp Business do tenant CORRENTE (Phone Number ID
 * + access token), só quando a integração está configurada e ativa -- mesmo
 * critério de "integração desligada" usado por e-SUS/SISREG/CNES (ver
 * IntegracaoConfigService). Precisa ser chamado dentro de uma transação com
 * o TenantContext já resolvido (o repositório é roteado por tenant).
 *
 * Compartilhado entre {@link LembreteAgendamentoTenantProcessor} (envio do
 * lembrete) e {@code WhatsappWebhookTenantProcessor} (resposta de
 * confirmação, enviada de volta ao paciente).
 */
@Component
public class WhatsappCredenciaisService {

    private final IntegracaoConfigRepository integracaoConfigRepository;

    public WhatsappCredenciaisService(IntegracaoConfigRepository integracaoConfigRepository) {
        this.integracaoConfigRepository = integracaoConfigRepository;
    }

    public Optional<Credenciais> ativas() {
        return integracaoConfigRepository.findByTipo(TipoIntegracao.WHATSAPP)
                .filter(IntegracaoConfig::isAtivo)
                .filter(config -> config.getBaseUrl() != null && !config.getBaseUrl().isBlank())
                .filter(config -> config.getToken() != null && !config.getToken().isBlank())
                .map(config -> new Credenciais(config.getBaseUrl(), config.getToken()));
    }

    public record Credenciais(String phoneNumberId, String accessToken) {
    }
}
