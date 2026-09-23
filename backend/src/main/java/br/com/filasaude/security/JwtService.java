package br.com.filasaude.security;

import br.com.filasaude.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Emissao e validacao de tokens JWT. O token carrega, alem do usuario (subject),
 * o papel (role) e o tenant — este ultimo usado para impedir que um token emitido
 * para a prefeitura A seja aceito em uma requisicao resolvida para a prefeitura B.
 */
@Service
public class JwtService {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TENANT = "tenant";
    public static final String CLAIM_NOME = "nome";
    /**
     * Marca um token como "pré-2FA": emitido logo após a senha ser validada,
     * mas antes do código do segundo fator. Tem vida curta (ver
     * {@link #EXPIRACAO_PRE_2FA_MINUTOS}) e o JwtAuthenticationFilter o trata
     * como não autenticado em qualquer rota além de /api/auth/2fa/validar-login
     * -- ele não concede acesso à API, só serve para "lembrar" quem passou na
     * senha enquanto aguarda o código do app autenticador.
     */
    public static final String CLAIM_PRE_2FA = "pre2fa";

    private static final long EXPIRACAO_PRE_2FA_MINUTOS = 5;

    /**
     * "Tenant" fixo e reservado para o painel de superadmin (fornecedor do
     * SaaS, item 3.6 — Fase 2). O superadmin não pertence a nenhuma
     * prefeitura: seu token usa este slug apenas para reaproveitar a mesma
     * checagem de tenant do token vs. tenant da requisição já existente em
     * {@link JwtAuthenticationFilter}, sem precisar de um caminho de
     * autenticação totalmente separado. Nenhum tenant real pode usar este
     * slug (ver validação em TenantProvisioningService).
     */
    public static final String SUPERADMIN_TENANT = "superadmin";
    public static final String ROLE_SUPERADMIN = "SUPERADMIN";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    @Autowired
    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(Usuario usuario, String tenantSlug) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + properties.getExpirationMinutes() * 60_000);

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim(CLAIM_ROLE, usuario.getPapel().name())
                .claim(CLAIM_TENANT, tenantSlug)
                .claim(CLAIM_NOME, usuario.getNome())
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Token de curta duração emitido após validar a senha de um usuário com
     * 2FA habilitado, aguardando o código do app autenticador (ver
     * {@link #CLAIM_PRE_2FA}). Não deve ser aceito pelo JwtAuthenticationFilter
     * em nenhuma outra rota.
     */
    public String gerarTokenPreAuth(Usuario usuario, String tenantSlug) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + EXPIRACAO_PRE_2FA_MINUTOS * 60_000);

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim(CLAIM_TENANT, tenantSlug)
                .claim(CLAIM_PRE_2FA, true)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Token do painel de superadmin — não está vinculado a nenhum {@link Usuario} de tenant. */
    public String gerarTokenSuperadmin(String email, String nome) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + properties.getExpirationMinutes() * 60_000);

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_ROLE, ROLE_SUPERADMIN)
                .claim(CLAIM_TENANT, SUPERADMIN_TENANT)
                .claim(CLAIM_NOME, nome)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validarEExtrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
