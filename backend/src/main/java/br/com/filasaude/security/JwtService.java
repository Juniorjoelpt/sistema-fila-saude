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

    public Claims validarEExtrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
