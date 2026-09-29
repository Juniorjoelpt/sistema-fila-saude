package br.com.filasaude.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Bug crítico de revisão corrigido: o segredo JWT (HS256) e o hash de senha
 * do superadmin tinham valores padrão de desenvolvimento embutidos no
 * código ({@link JwtProperties#getSecret()} / {@link SuperadminProperties#getPasswordHash()}),
 * usados silenciosamente sempre que as variáveis de ambiente (JWT_SECRET /
 * SUPERADMIN_PASSWORD_HASH) não fossem definidas. Um erro de configuração de
 * deploy (esquecer de setar essas variáveis) permitia a qualquer pessoa com
 * acesso ao código-fonte forjar um JWT válido para qualquer papel/tenant, ou
 * logar no painel de superadmin com a senha pública "admin123".
 *
 * Este validador roda no boot e derruba a aplicação com uma mensagem clara
 * se o perfil ativo for de produção (spring.profiles.active contendo
 * "prod"/"production") e algum desses defaults inseguros ainda estiver em
 * uso -- em vez de subir silenciosamente com uma falha de segurança grave.
 * Em qualquer outro ambiente (dev local, testes), apenas loga um aviso, para
 * não atrapalhar o fluxo de desenvolvimento local descrito no README.
 */
@Component
public class SegurancaProducaoValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SegurancaProducaoValidator.class);

    private static final String JWT_SECRET_PADRAO =
            "changeme-super-secret-key-min-32-chars-long-for-hs256";
    private static final String SUPERADMIN_HASH_PADRAO =
            "$2b$10$6tpFjLKqg2ySy4ZUMmQw4ehzGjOsg3sWpjshhvqjiliNe7uvsIeEy";
    private static final Set<String> PERFIS_PRODUCAO = Set.of("prod", "production", "producao");

    private final JwtProperties jwtProperties;
    private final SuperadminProperties superadminProperties;
    private final Environment environment;

    public SegurancaProducaoValidator(JwtProperties jwtProperties,
                                       SuperadminProperties superadminProperties,
                                       Environment environment) {
        this.jwtProperties = jwtProperties;
        this.superadminProperties = superadminProperties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean usaJwtPadrao = JWT_SECRET_PADRAO.equals(jwtProperties.getSecret());
        boolean usaSuperadminPadrao = SUPERADMIN_HASH_PADRAO.equals(superadminProperties.getPasswordHash());

        if (!usaJwtPadrao && !usaSuperadminPadrao) {
            return;
        }

        boolean ambienteDeProducao = estaEmProducao();

        if (usaJwtPadrao) {
            String mensagem = "JWT_SECRET não foi definido: a aplicação está usando o segredo padrão de "
                    + "desenvolvimento, publicamente conhecido (está no código-fonte). Qualquer pessoa pode "
                    + "forjar tokens de acesso válidos para qualquer papel/tenant enquanto isso não for corrigido.";
            if (ambienteDeProducao) {
                throw new IllegalStateException(mensagem + " Defina a variável de ambiente JWT_SECRET "
                        + "antes de subir em produção (perfil ativo: " + String.join(",", environment.getActiveProfiles()) + ").");
            }
            log.warn(mensagem);
        }

        if (usaSuperadminPadrao) {
            String mensagem = "SUPERADMIN_PASSWORD_HASH não foi definido: a senha do painel de superadmin "
                    + "continua sendo a senha padrão de desenvolvimento (\"admin123\"), publicamente conhecida.";
            if (ambienteDeProducao) {
                throw new IllegalStateException(mensagem + " Defina a variável de ambiente SUPERADMIN_PASSWORD_HASH "
                        + "(hash BCrypt) antes de subir em produção (perfil ativo: " + String.join(",", environment.getActiveProfiles()) + ").");
            }
            log.warn(mensagem);
        }
    }

    private boolean estaEmProducao() {
        List<String> perfisAtivos = List.of(environment.getActiveProfiles());
        return perfisAtivos.stream().map(String::toLowerCase).anyMatch(PERFIS_PRODUCAO::contains);
    }
}
