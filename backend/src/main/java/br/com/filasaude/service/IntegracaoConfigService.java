package br.com.filasaude.service;

import br.com.filasaude.domain.IntegracaoConfig;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.domain.enums.TipoIntegracao;
import br.com.filasaude.dto.integracao.IntegracaoConfigRequest;
import br.com.filasaude.dto.integracao.IntegracaoConfigResponse;
import br.com.filasaude.repository.IntegracaoConfigRepository;
import br.com.filasaude.repository.UsuarioRepository;
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

    public IntegracaoConfigService(IntegracaoConfigRepository integracaoConfigRepository,
                                    UsuarioRepository usuarioRepository) {
        this.integracaoConfigRepository = integracaoConfigRepository;
        this.usuarioRepository = usuarioRepository;
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
        return IntegracaoConfigResponse.de(tipo, salvo);
    }

    private Optional<Usuario> usuarioLogado() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .flatMap(usuarioRepository::findByEmailIgnoreCase);
    }
}
