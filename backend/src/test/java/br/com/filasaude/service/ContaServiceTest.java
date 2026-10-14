package br.com.filasaude.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContaServiceTest {

    @Test
    void aceitaSenhaComOitoOuMaisCaracteresComLetrasENumeros() {
        assertThatCode(() -> ContaService.validarNovaSenha("Segura2026", "provisoria1")).doesNotThrowAnyException();
    }

    @Test
    void recusaSenhaCurta() {
        assertThatThrownBy(() -> ContaService.validarNovaSenha("abc123", "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("8 caracteres");
    }

    @Test
    void recusaSenhaSemNumeroOuSemLetra() {
        assertThatThrownBy(() -> ContaService.validarNovaSenha("somenteletras", "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("letras e números");
        assertThatThrownBy(() -> ContaService.validarNovaSenha("1234567890", "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("letras e números");
    }

    @Test
    void recusaSenhaIgualAAtual() {
        assertThatThrownBy(() -> ContaService.validarNovaSenha("Segura2026", "Segura2026"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("diferente");
    }
}
