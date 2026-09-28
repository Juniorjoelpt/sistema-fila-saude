package br.com.filasaude.service;

import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.PresencaConfirmacao;
import br.com.filasaude.dto.publico.ConfirmacaoPresencaResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.ProtocoloRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static br.com.filasaude.support.Fixtures.paciente;
import static br.com.filasaude.support.Fixtures.procedimento;
import static br.com.filasaude.support.Fixtures.protocolo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Confirmação/cancelamento de presença pelo link público de lembrete --
 * identidade provada apenas pela posse do token, sem exigir login.
 */
@ExtendWith(MockitoExtension.class)
class ProtocoloConfirmacaoServiceTest {

    @Mock
    private ProtocoloRepository protocoloRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private ProtocoloConfirmacaoService service() {
        return new ProtocoloConfirmacaoService(protocoloRepository, auditoriaService);
    }

    private final Paciente paciente = paciente(1L, "Paciente Teste");
    private final Procedimento procedimento = procedimento(1L, "Consulta Cardiologia", "Cardiologia");

    @Test
    void confirmarComTokenInvalidoLancaResourceNotFound() {
        when(protocoloRepository.findByConfirmacaoToken("token-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().confirmar("token-invalido"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelarComTokenInvalidoLancaResourceNotFound() {
        when(protocoloRepository.findByConfirmacaoToken("token-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().cancelar("token-invalido"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirmarMarcaPresencaConfirmadaERegistraAuditoria() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setConfirmacaoToken("token-valido");
        when(protocoloRepository.findByConfirmacaoToken("token-valido")).thenReturn(Optional.of(protocolo));

        ConfirmacaoPresencaResponse response = service().confirmar("token-valido");

        assertThat(protocolo.getPresencaConfirmacao()).isEqualTo(PresencaConfirmacao.CONFIRMADA);
        assertThat(response.presencaConfirmacao()).isEqualTo("CONFIRMADA");
        assertThat(response.numeroProtocolo()).isEqualTo(protocolo.getNumeroProtocolo());
        verify(auditoriaService).registrar(eq("CONFIRMAR_PRESENCA"), eq("Protocolo"), eq(10L), anyString());
    }

    @Test
    void cancelarMarcaPresencaCanceladaERegistraAuditoria() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setConfirmacaoToken("token-valido");
        when(protocoloRepository.findByConfirmacaoToken("token-valido")).thenReturn(Optional.of(protocolo));

        ConfirmacaoPresencaResponse response = service().cancelar("token-valido");

        assertThat(protocolo.getPresencaConfirmacao()).isEqualTo(PresencaConfirmacao.CANCELADA);
        assertThat(response.presencaConfirmacao()).isEqualTo("CANCELADA");
        verify(auditoriaService).registrar(eq("CANCELAR_PRESENCA"), eq("Protocolo"), eq(10L), anyString());
    }

    @Test
    void consultarNaoAlteraPresencaEDevolveNullQuandoAindaNaoRespondeu() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setConfirmacaoToken("token-valido");
        when(protocoloRepository.findByConfirmacaoToken("token-valido")).thenReturn(Optional.of(protocolo));

        ConfirmacaoPresencaResponse response = service().consultar("token-valido");

        assertThat(response.presencaConfirmacao()).isNull();
        assertThat(response.nomeUnidadeSaude()).isNull();
    }
}
