package br.com.filasaude.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciais do painel de superadmin (fornecedor do SaaS — item 3.6, Fase
 * 2). Não é um {@code Usuario} de tenant: é uma credencial única, fixa,
 * definida por variável de ambiente em produção. O hash de senha padrão
 * abaixo corresponde à senha "admin123", apenas para ambiente de
 * desenvolvimento local — DEVE ser sobrescrito em produção.
 */
@ConfigurationProperties(prefix = "filasaude.superadmin")
public class SuperadminProperties {

    private String email = "superadmin@filasaude.com.br";
    private String nome = "Suporte MS Soluções";

    /** BCrypt de "admin123" — troque via SUPERADMIN_PASSWORD_HASH em produção. */
    private String passwordHash = "$2b$10$6tpFjLKqg2ySy4ZUMmQw4ehzGjOsg3sWpjshhvqjiliNe7uvsIeEy";

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
