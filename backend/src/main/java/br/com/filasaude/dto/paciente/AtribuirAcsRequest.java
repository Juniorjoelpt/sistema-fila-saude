package br.com.filasaude.dto.paciente;

/**
 * Reatribuição do ACS responsável por um paciente (privativo de
 * Regulador/Admin -- ver SecurityConfig). {@code acsResponsavelId} nulo
 * desfaz o vínculo (paciente volta a ficar sem ACS responsável).
 */
public record AtribuirAcsRequest(Long acsResponsavelId) {
}
