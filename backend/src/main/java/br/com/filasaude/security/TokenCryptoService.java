package br.com.filasaude.security;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Bug alto de revisão corrigido: tokens de integração (e-SUS, SISREG, CNES,
 * WhatsApp -- credenciais de acesso a APIs oficiais do Ministério da Saúde)
 * eram gravados em TEXTO PURO na coluna `token` de `integracoes_config`.
 * Qualquer um com acesso de leitura ao banco (um dump de backup, uma query
 * de suporte) via a credencial em claro. Criptografa com AES-256-GCM antes
 * de salvar e descriptografa só quando o valor precisa ser realmente usado
 * (nunca é devolvido para o frontend -- ver IntegracaoConfigResponse).
 *
 * A chave é derivada (SHA-256) do mesmo segredo já usado para assinar JWTs
 * (filasaude.jwt.secret) em vez de introduzir uma variável de ambiente nova
 * -- evita quebrar deployments existentes que ainda não conhecem uma chave
 * de criptografia separada. Registrado aqui como possível melhoria futura:
 * usar uma chave dedicada (rotação independente da chave de JWT).
 */
@Component
public class TokenCryptoService {

    private static final String PREFIXO = "enc:v1:";
    private static final int TAMANHO_IV = 12;
    private static final int TAMANHO_TAG_BITS = 128;

    private final SecretKeySpec chave;
    private final SecureRandom random = new SecureRandom();

    public TokenCryptoService(JwtProperties jwtProperties) {
        this.chave = derivarChave(jwtProperties.getSecret());
    }

    private static SecretKeySpec derivarChave(String segredo) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] bytesChave = sha256.digest(segredo.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(bytesChave, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível derivar a chave de criptografia de tokens", e);
        }
    }

    public String criptografar(String textoPuro) {
        if (textoPuro == null || textoPuro.isBlank()) {
            return textoPuro;
        }
        try {
            byte[] iv = new byte[TAMANHO_IV];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(textoPuro.getBytes(StandardCharsets.UTF_8));

            byte[] ivMaisCifrado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, ivMaisCifrado, 0, iv.length);
            System.arraycopy(cifrado, 0, ivMaisCifrado, iv.length, cifrado.length);

            return PREFIXO + Base64.getEncoder().encodeToString(ivMaisCifrado);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível criptografar o token de integração", e);
        }
    }

    public String descriptografar(String valorArmazenado) {
        if (valorArmazenado == null || valorArmazenado.isBlank()) {
            return valorArmazenado;
        }
        if (!valorArmazenado.startsWith(PREFIXO)) {
            // Valor legado, gravado antes desta correção (texto puro) -- devolve
            // como está para não quebrar integrações já configuradas; será
            // recriptografado na próxima vez que o token for salvo.
            return valorArmazenado;
        }
        try {
            byte[] ivMaisCifrado = Base64.getDecoder().decode(valorArmazenado.substring(PREFIXO.length()));
            byte[] iv = new byte[TAMANHO_IV];
            System.arraycopy(ivMaisCifrado, 0, iv, 0, TAMANHO_IV);
            byte[] cifrado = new byte[ivMaisCifrado.length - TAMANHO_IV];
            System.arraycopy(ivMaisCifrado, TAMANHO_IV, cifrado, 0, cifrado.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] textoPuro = cipher.doFinal(cifrado);
            return new String(textoPuro, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível descriptografar o token de integração", e);
        }
    }
}
