package br.com.filasaude.security;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Implementacao própria do TOTP (RFC 6238, o mesmo algoritmo usado por Google
 * Authenticator, Authy, Microsoft Authenticator etc.), usando apenas
 * javax.crypto (já disponível no JDK) -- sem depender de uma biblioteca de
 * terceiros para a parte criptográfica, só para o desenho do QR Code (ZXing).
 *
 * Sem estado: o segredo de cada usuário fica em Usuario.twoFactorSecret,
 * gerenciado por TwoFactorService.
 */
@Service
public class TotpService {

    private static final String ALGORITMO_HMAC = "HmacSHA1";
    private static final int DIGITOS = 6;
    private static final int PASSO_SEGUNDOS = 30;
    // Tolera 1 passo (30s) para tras/frente, para absorver pequena diferenca
    // de relogio entre o celular do usuario e o servidor.
    private static final int TOLERANCIA_PASSOS = 1;
    private static final String BASE32_ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int TAMANHO_QRCODE_PX = 260;

    private final SecureRandom secureRandom = new SecureRandom();

    /** Gera um novo segredo aleatório de 160 bits (recomendado para HMAC-SHA1), em Base32. */
    public String gerarSecret() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * Gera o QR Code (PNG, em Base64) do link otpauth:// que o app autenticador
     * lê para importar o segredo. O rótulo mostrado no app combina o nome do
     * sistema com o e-mail do usuário, para diferenciar de outras contas
     * cadastradas no mesmo app.
     */
    public String gerarQrCodeBase64(String secretBase32, String emailUsuario, String issuer) {
        String otpauthUrl = "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d"
                .formatted(
                        urlEncode(issuer), urlEncode(emailUsuario), secretBase32,
                        urlEncode(issuer), DIGITOS, PASSO_SEGUNDOS);
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(otpauthUrl, BarcodeFormat.QR_CODE, TAMANHO_QRCODE_PX, TAMANHO_QRCODE_PX);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Não foi possível gerar o QR Code do 2FA.", e);
        }
    }

    /** Valida um código de 6 dígitos digitado pelo usuário contra o segredo, com tolerância de relógio. */
    public boolean validarCodigo(String secretBase32, String codigo) {
        if (secretBase32 == null || codigo == null || !codigo.matches("\\d{6}")) {
            return false;
        }
        long passoAtual = System.currentTimeMillis() / 1000L / PASSO_SEGUNDOS;
        byte[] chave = base32Decode(secretBase32);
        for (int i = -TOLERANCIA_PASSOS; i <= TOLERANCIA_PASSOS; i++) {
            if (gerarCodigoParaPasso(chave, passoAtual + i).equals(codigo)) {
                return true;
            }
        }
        return false;
    }

    private String gerarCodigoParaPasso(byte[] chave, long passo) {
        try {
            byte[] dadosPasso = new byte[8];
            long valor = passo;
            for (int i = 7; i >= 0; i--) {
                dadosPasso[i] = (byte) (valor & 0xff);
                valor >>= 8;
            }

            Mac mac = Mac.getInstance(ALGORITMO_HMAC);
            mac.init(new SecretKeySpec(chave, ALGORITMO_HMAC));
            byte[] hash = mac.doFinal(dadosPasso);

            int offset = hash[hash.length - 1] & 0x0f;
            int binario = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);

            int otp = binario % (int) Math.pow(10, DIGITOS);
            return String.format("%0" + DIGITOS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao calcular código TOTP", e);
        }
    }

    private String urlEncode(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String base32Encode(byte[] dados) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bufferBits = 0;
        for (byte b : dados) {
            buffer = (buffer << 8) | (b & 0xff);
            bufferBits += 8;
            while (bufferBits >= 5) {
                int indice = (buffer >> (bufferBits - 5)) & 0x1f;
                sb.append(BASE32_ALFABETO.charAt(indice));
                bufferBits -= 5;
            }
        }
        if (bufferBits > 0) {
            int indice = (buffer << (5 - bufferBits)) & 0x1f;
            sb.append(BASE32_ALFABETO.charAt(indice));
        }
        return sb.toString();
    }

    private byte[] base32Decode(String base32) {
        String limpo = base32.trim().toUpperCase().replace("=", "");
        int buffer = 0;
        int bufferBits = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (char c : limpo.toCharArray()) {
            int indice = BASE32_ALFABETO.indexOf(c);
            if (indice < 0) {
                continue;
            }
            buffer = (buffer << 5) | indice;
            bufferBits += 5;
            if (bufferBits >= 8) {
                out.write((buffer >> (bufferBits - 8)) & 0xff);
                bufferBits -= 8;
            }
        }
        return out.toByteArray();
    }
}
