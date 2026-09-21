package br.com.filasaude.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "filasaude.jwt")
public class JwtProperties {

    /** Chave secreta HS256 — minimo 32 caracteres. Definir via variavel de ambiente em produção. */
    private String secret = "changeme-super-secret-key-min-32-chars-long-for-hs256";

    private long expirationMinutes = 480; // 8h de expediente

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
}
