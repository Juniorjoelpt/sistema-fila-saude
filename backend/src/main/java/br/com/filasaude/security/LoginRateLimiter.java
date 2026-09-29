package br.com.filasaude.security;

import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bug alto de revisão corrigido: não existia nenhum limite de tentativas de
 * login. Um atacante podia tentar senhas indefinidamente contra qualquer
 * conta (admin, superadmin) sem nenhum atraso ou bloqueio -- um brute force
 * clássico. Este componente aplica um bloqueio temporário por
 * tenant+identificador (email) após várias falhas seguidas.
 *
 * Implementação em memória (ConcurrentHashMap), suficiente para uma única
 * instância da aplicação, que é o modelo de deployment atual (ver
 * docker-compose.yml -- um único container do backend). Se o sistema vier a
 * rodar em múltiplas instâncias, isso precisaria migrar para um armazenamento
 * compartilhado (ex.: Redis); registrado aqui para não perder o contexto.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_TENTATIVAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private static final class Estado {
        final AtomicInteger falhas = new AtomicInteger(0);
        volatile Instant primeiraFalha = Instant.now();
        volatile Instant bloqueadoAte;
    }

    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

    /** Lança {@link LockedException} se a chave (tenant+email) estiver temporariamente bloqueada. */
    public void verificarNaoBloqueado(String tenant, String identificador) {
        Estado estado = estados.get(chave(tenant, identificador));
        if (estado == null) {
            return;
        }
        Instant bloqueadoAte = estado.bloqueadoAte;
        if (bloqueadoAte != null) {
            if (Instant.now().isBefore(bloqueadoAte)) {
                throw new LockedException(
                        "Muitas tentativas de login com credenciais inválidas. Tente novamente em alguns minutos.");
            }
            // Janela de bloqueio expirou: reseta o contador.
            estados.remove(chave(tenant, identificador));
        }
    }

    /** Registra uma falha de autenticação; bloqueia a chave se o limite for atingido. */
    public void registrarFalha(String tenant, String identificador) {
        Estado estado = estados.computeIfAbsent(chave(tenant, identificador), k -> new Estado());

        if (Duration.between(estado.primeiraFalha, Instant.now()).compareTo(JANELA) > 0) {
            estado.falhas.set(0);
            estado.primeiraFalha = Instant.now();
        }

        int total = estado.falhas.incrementAndGet();
        if (total >= MAX_TENTATIVAS) {
            estado.bloqueadoAte = Instant.now().plus(BLOQUEIO);
        }
    }

    /** Limpa o contador após um login bem-sucedido. */
    public void registrarSucesso(String tenant, String identificador) {
        estados.remove(chave(tenant, identificador));
    }

    private String chave(String tenant, String identificador) {
        return (tenant == null ? "-" : tenant) + "::" + (identificador == null ? "-" : identificador.toLowerCase());
    }
}
