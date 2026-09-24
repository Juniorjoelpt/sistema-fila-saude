package br.com.filasaude.service;

import br.com.filasaude.domain.IntegracaoConfig;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.domain.enums.TipoIntegracao;
import br.com.filasaude.dto.integracao.IntegracaoConfigRequest;
import br.com.filasaude.dto.integracao.IntegracaoConfigResponse;
import br.com.filasaude.repository.IntegracaoConfigRepository;
import br.com.filasaude.repository.UsuarioRepository;
import br.com.filasaude.tenancy.MasterWhatsappNumeroRepository;
import br.com.filasaude.tenancy.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Configuração das integrações obrigatórias com sistemas do Ministério da
 * Saúde (decisão confirmada no levantamento de requisitos): e-SUS, SISREG e
 * CNES. Guarda credenciais por tenant; a integração em si (chamadas reais às
 * APIs oficiais) fica para quando o cliente tiver acesso a essas APIs -- ver
 * br.com.filasaude.integracao para os adaptadores preparados e ainda não
 * conectados.
 */
@Service
@Transactional
public class IntegracaoConfigService {

    private final IntegracaoConfigRepository integracaoConfigRepository;
    private final UsuarioRepository usuarioRepository;
    private final MasterWhatsappNumeroRepository masterWhatsappNumeroRepository;

    public IntegracaoConfigService(IntegracaoConfigRepository integracaoConfigRepository,
                                    UsuarioRepository usuarioRepository,
                                    MasterWhatsappNumeroRepository masterWhatsappNumeroRepository) {
        this.integracaoConfigRepository = integracaoConfigRepository;
        this.usuarioRepository = usuarioRepository;
        this.masterWhatsappNumeroRepository = masterWhatsappNumeroRepository;
    }

    @Transactional(readOnly = true)
    public List<IntegracaoConfigResponse> listar() {
        return Arrays.stream(TipoIntegracao.values())
                .map(tipo -> IntegracaoConfigResponse.de(tipo, integracaoConfigRepository.findByTipo(tipo).orElse(null)))
                .toList();
    }

    public IntegracaoConfigResponse salvar(TipoIntegracao tipo, IntegracaoConfigRequest request) {
        IntegracaoConfig config = integracaoConfigRepository.findByTipo(tipo)
                .orElseGet(() -> IntegracaoConfig.builder().tipo(tipo).build());
        String baseUrlAnterior = config.getBaseUrl();

        config.setBaseUrl(request.baseUrl());
        // Token em branco mantém o token já salvo -- permite ativar/desativar
        // ou trocar só a URL sem precisar reenviar a credencial toda vez.
        if (request.token() != null && !request.token().isBlank()) {
            config.setToken(request.token());
        }
        config.setAtivo(request.ativo());
        config.setAtualizadoEm(LocalDateTime.now());
        config.setAtualizadoPor(usuarioLogado().orElse(null));

        IntegracaoConfig salvo = integracaoConfigRepository.save(config);

        if (tipo == TipoIntegracao.WHATSAPP) {
            sincronizarMapeamentoWhatsapp(salvo, baseUrlAnterior);
        }

        return IntegracaoConfigResponse.de(tipo, salvo);
    }

    /**
     * Mantém a tabela `whatsapp_numeros` do banco MASTER em sincronia com a
     * configuração deste tenant (ver WhatsappWebhookController, que depende
     * dela para descobrir a qual prefeitura um evento recebido pertence). O
     * "baseUrl" da integração WHATSAPP é o Phone Number ID.
     */
    private void sincronizarMapeamentoWhatsapp(IntegracaoConfig config, String baseUrlAnterior) {
        String tenantSlug = TenantContext.getCurrentTenant();
        String phoneNumberIdAtual = config.getBaseUrl();

        // Número trocado: remove o mapeamento antigo antes de registrar o novo.
        if (baseUrlAnterior != null && !baseUrlAnterior.isBlank() && !baseUrlAnterior.equals(phoneNumberIdAtual)) {
            masterWhatsappNumeroRepository.remover(baseUrlAnterior);
        }

        boolean configuradoECompleto = config.isAtivo()
                && phoneNumberIdAtual != null && !phoneNumberIdAtual.isBlank()
                && config.getToken() != null && !config.getToken().isBlank();

        if (configuradoECompleto) {
            masterWhatsappNumeroRepository.registrar(phoneNumberIdAtual, tenantSlug);
        } else if (phoneNumberIdAtual != null && !phoneNumberIdAtual.isBlank()) {
            masterWhatsappNumeroRepository.remover(phoneNumberIdAtual);
        }
    }

    private Optional<Usuario> usuarioLogado() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .flatMap(usuarioRepository::findByEmailIgnoreCase);
    }
}
