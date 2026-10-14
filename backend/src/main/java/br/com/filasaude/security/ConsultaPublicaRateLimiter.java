package br.com.filasaude.security;

import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita as tentativas FALHAS de consulta pública de protocolo (CPF/CNS +
 * data de nascimento) por origem+prefeitura. Sem isso, qualquer pessoa podia
 * testar CPFs em sequência e descobrir, pela diferença entre 200 e 404, quem
 * é paciente da prefeitura -- dado de saúde (LGPD, art. 11).
 *
 * Em memória, como o {@link LoginRateLimiter} (uma única instância do backend).
 */
@Component
public class ConsultaPublicaRateLimiter {

    private static final int MAX_FALHAS = 8;
    private static final Duration JANELA = Duration.ofMinutes(15);

    private static final class Estado {
        int falhas;
        Instant inicio = Instant.now();
    }

    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

    public void verificar(String tenant, String origem) {
        Estado estado = estados.get(chave(tenant, origem));
        if (estado == null) {
            return;
        }
        synchronized (estado) {
            if (Duration.between(estado.inicio, Instant.now()).compareTo(JANELA) > 0) {
                estados.remove(chave(tenant, origem));
                return;
            }
            if (estado.falhas >= MAX_FALHAS) {
                throw new LockedException(
                        "Muitas consultas sem resultado. Aguarde alguns minutos e confira os dados informados.");
            }
        }
    }

    public void registrarFalha(String tenant, String origem) {
        Estado estado = estados.computeIfAbsent(chave(tenant, origem), k -> new Estado());
        synchronized (estado) {
            if (Duration.between(estado.inicio, Instant.now()).compareTo(JANELA) > 0) {
                estado.falhas = 0;
                estado.inicio = Instant.now();
            }
            estado.falhas++;
        }
    }

    public void registrarSucesso(String tenant, String origem) {
        estados.remove(chave(tenant, origem));
    }

    private String chave(String tenant, String origem) {
        return (tenant == null ? "-" : tenant) + "::" + (origem == null ? "-" : origem);
    }
}
