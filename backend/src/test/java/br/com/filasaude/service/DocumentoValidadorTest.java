package br.com.filasaude.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentoValidadorTest {

    @Test
    void cpfComDigitosVerificadoresCorretosEValido() {
        assertThat(DocumentoValidador.cpfValido("52998224725")).isTrue();
        assertThat(DocumentoValidador.cpfValido("11144477735")).isTrue();
    }

    @Test
    void cpfComDigitoErradoOuRepetidoOuTamanhoErradoEInvalido() {
        assertThat(DocumentoValidador.cpfValido("12345678900")).isFalse();
        assertThat(DocumentoValidador.cpfValido("52998224726")).isFalse();
        assertThat(DocumentoValidador.cpfValido("11111111111")).isFalse();
        assertThat(DocumentoValidador.cpfValido("123")).isFalse();
        assertThat(DocumentoValidador.cpfValido(null)).isFalse();
    }

    @Test
    void cnsProvisorioComSomaMultiplaDe11EValido() {
        assertThat(DocumentoValidador.cnsValido("898001160660001")).isTrue();
        assertThat(DocumentoValidador.cnsValido("898001160660002")).isFalse();
    }

    @Test
    void cnsDefinitivoComDigitoVerificadorEValido() {
        assertThat(DocumentoValidador.cnsValido("100000000000007")).isTrue();
        assertThat(DocumentoValidador.cnsValido("123456789010000")).isTrue();
        assertThat(DocumentoValidador.cnsValido("200000000010009")).isTrue();
        assertThat(DocumentoValidador.cnsValido("100000000000008")).isFalse();
        assertThat(DocumentoValidador.cnsValido("123456789012345")).isFalse();
        assertThat(DocumentoValidador.cnsValido("1234")).isFalse();
        assertThat(DocumentoValidador.cnsValido("300000000000000")).isFalse();
    }
}
