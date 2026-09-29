package br.com.filasaude.service;

import br.com.filasaude.domain.IntegracaoConfig;
import br.com.filasaude.domain.enums.TipoIntegracao;
import br.com.filasaude.repository.IntegracaoConfigRepository;
import br.com.filasaude.security.TokenCryptoService;
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
    private final TokenCryptoService tokenCryptoService;

    public WhatsappCredenciaisService(IntegracaoConfigRepository integracaoConfigRepository,
                                       TokenCryptoService tokenCryptoService) {
        this.integracaoConfigRepository = integracaoConfigRepository;
        this.tokenCryptoService = tokenCryptoService;
    }

    public Optional<Credenciais> ativas() {
        return integracaoConfigRepository.findByTipo(TipoIntegracao.WHATSAPP)
                .filter(IntegracaoConfig::isAtivo)
                .filter(config -> config.getBaseUrl() != null && !config.getBaseUrl().isBlank())
                .filter(config -> config.getToken() != null && !config.getToken().isBlank())
                // O token é gravado criptografado (ver TokenCryptoService) --
                // precisa ser descriptografado aqui, no único lugar em que o
                // valor real é usado (chamada à API do WhatsApp).
                .map(config -> new Credenciais(config.getBaseUrl(), tokenCryptoService.descriptografar(config.getToken())));
    }

    public record Credenciais(String phoneNumberId, String accessToken) {
    }
}
