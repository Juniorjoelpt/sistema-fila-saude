package br.com.filasaude.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resolve o tenant (prefeitura) da requisição, nesta ordem de prioridade:
 *   1. Header explícito "X-Tenant-Id" (usado pelo frontend em todas as chamadas,
 *      inclusive nas rotas públicas de consulta de protocolo).
 *   2. Subdomínio do Host (ex.: portopi.filasaude.com.br -> tenant "portopi"),
 *      fallback útil quando o front aponta direto para o domínio da prefeitura.
 *
 * IMPORTANTE: este filtro NÃO é um @Component/@Order genérico de propósito.
 * Um filtro registrado dessa forma entra na cadeia de filtros do servlet
 * container com prioridade mais baixa que a cadeia do Spring Security
 * (que roda com ordem -100), ou seja, rodaria DEPOIS do JwtAuthenticationFilter
 * -- e este último precisa do tenant já resolvido (TenantContext) para validar
 * a claim "tenant" do token. Por isso o filtro é registrado manualmente dentro
 * da cadeia do Spring Security, antes do JwtAuthenticationFilter (ver
 * SecurityConfig.filterChain).
 */
public class TenantResolverFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    private final TenantDataSourceRegistry registry;

    public TenantResolverFilter(TenantDataSourceRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String tenant = resolveTenant(request);
            if (tenant != null) {
                TenantContext.setCurrentTenant(tenant);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenant(HttpServletRequest request) {
        String header = request.getHeader(TENANT_HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim().toLowerCase();
        }

        String host = request.getServerName();
        if (host != null && host.contains(".")) {
            String subdomain = host.substring(0, host.indexOf('.'));
            if (!subdomain.isBlank() && !"www".equalsIgnoreCase(subdomain)) {
                return subdomain.toLowerCase();
            }
        }
        return null;
    }
}
