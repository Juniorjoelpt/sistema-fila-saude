package br.com.filasaude.dto.twofactor;

/**
 * secretBase32: mostrado como texto para quem prefere digitar manualmente no
 * app autenticador em vez de escanear o QR Code (ex.: celular sem câmera
 * disponível no momento).
 * qrCodeBase64Png: PNG do QR Code já em Base64, pronto para um <img
 * src="data:image/png;base64,...">.
 */
public record TwoFactorSetupResponse(
        String secretBase32,
        String qrCodeBase64Png
) {
}
