package br.com.filasaude.dto.auth;

/**
 * Resposta de POST /api/auth/login. Duas formas possíveis, distinguidas por
 * requerDoisFatores:
 * - requerDoisFatores=true: credenciais corretas, mas o usuário tem 2FA
 *   habilitado. loginToken vem preenchido (vida curta, só serve para
 *   POST /api/auth/2fa/validar-login) e os demais campos vêm nulos -- ainda
 *   não é um login completo.
 * - requerDoisFatores=false: login concluído. token vem preenchido (JWT
 *   normal da sessão) junto dos dados do usuário; loginToken vem nulo.
 */
public record LoginResponse(
        boolean requerDoisFatores,
        String loginToken,
        String token,
        String nome,
        String email,
        String papel
) {
    public static LoginResponse desafio2fa(String loginToken) {
        return new LoginResponse(true, loginToken, null, null, null, null);
    }

    public static LoginResponse concluido(String token, String nome, String email, String papel) {
        return new LoginResponse(false, null, token, nome, email, papel);
    }
}
