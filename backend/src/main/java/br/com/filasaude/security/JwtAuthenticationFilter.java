package br.com.filasaude.security;

import br.com.filasaude.tenancy.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Valida o Bearer token em cada requisicao autenticada. Roda depois do
 * TenantResolverFilter (registrado explicitamente antes deste filtro em
 * SecurityConfig.filterChain), pois compara o claim "tenant" do token
 * com o tenant ja resolvido da requisicao — um token emitido para a prefeitura A
 * nao e aceito numa chamada resolvida para a prefeitura B.
 *
 * Validacao e "stateless": nao consulta o banco a cada requisicao, confia nas
 * claims assinadas (email, role, tenant). Suficiente para o MVP; uma verificacao
 * de usuario ainda ativo pode ser adicionada depois via cache curto, se necessario.
 */
@Component
@Order(2)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.validarEExtrairClaims(token);
                String tenantDoToken = claims.get(JwtService.CLAIM_TENANT, String.class);
                String tenantDaRequisicao = TenantContext.getCurrentTenant();

                if (tenantDoToken != null && tenantDoToken.equals(tenantDaRequisicao)) {
                    String role = claims.get(JwtService.CLAIM_ROLE, String.class);
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var authentication = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("Token rejeitado: tenant do token ({}) difere do tenant da requisicao ({})",
                            tenantDoToken, tenantDaRequisicao);
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("Token invalido ou expirado: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
