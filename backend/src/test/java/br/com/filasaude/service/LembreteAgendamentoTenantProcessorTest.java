package br.com.filasaude.service;

import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.PresencaConfirmacao;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.repository.ProtocoloRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static br.com.filasaude.support.Fixtures.paciente;
import static br.com.filasaude.support.Fixtures.procedimento;
import static br.com.filasaude.support.Fixtures.protocolo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Varredura de lembrete de agendamento (item "Lembrete + confirmação de
 * presença"): gera token de confirmação, marca PENDENTE, e-mail sempre
 * dispara, WhatsApp só dispara se o tenant tiver credenciais ativas
 * configuradas (canal opcional -- ver tela de Integrações).
 */
@ExtendWith(MockitoExtension.class)
class LembreteAgendamentoTenantProcessorTest {

    @Mock private ProtocoloRepository protocoloRepository;
    @Mock private WhatsappCredenciaisService whatsappCredenciaisService;
    @Mock private NotificacaoEmailService notificacaoEmailService;
    @Mock private NotificacaoWhatsappService notificacaoWhatsappService;

    private LembreteAgendamentoTenantProcessor processor(int diasAntes) {
        return new LembreteAgendamentoTenantProcessor(protocoloRepository, whatsappCredenciaisService,
                notificacaoEmailService, notificacaoWhatsappService, diasAntes);
    }

    private final Paciente paciente = paciente(1L, "Paciente Teste");
    private final Procedimento procedimento = procedimento(1L, "Consulta Cardiologia", "Cardiologia");

    @Test
    void naoFazNadaQuandoNaoHaProtocolosParaLembrar() {
        when(protocoloRepository.findParaLembreteAgendamento(eq(StatusProtocolo.AGENDADO), any(LocalDate.class)))
                .thenReturn(List.of());

        processor(1).processarTenantCorrente("prefeitura-x");

        verify(protocoloRepository, never()).saveAll(any());
        verify(notificacaoEmailService, never()).notificarLembreteAgendamento(any(), anyString());
        verify(whatsappCredenciaisService, never()).ativas();
    }

    @Test
    void geraTokenMarcaPendenteESempreDisparaEmailQuandoHaProtocolos() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setStatus(StatusProtocolo.AGENDADO);
        when(protocoloRepository.findParaLembreteAgendamento(eq(StatusProtocolo.AGENDADO), any(LocalDate.class)))
                .thenReturn(List.of(protocolo));
        when(whatsappCredenciaisService.ativas()).thenReturn(Optional.empty());

        processor(1).processarTenantCorrente("prefeitura-x");

        assertThat(protocolo.getConfirmacaoToken()).isNotBlank();
        assertThat(protocolo.getPresencaConfirmacao()).isEqualTo(PresencaConfirmacao.PENDENTE);
        assertThat(protocolo.getLembreteEnviadoEm()).isNotNull();
        verify(protocoloRepository).saveAll(List.of(protocolo));
        verify(notificacaoEmailService).notificarLembreteAgendamento(protocolo, "prefeitura-x");
        verify(notificacaoWhatsappService, never()).notificarLembreteAgendamento(any(), anyString(), anyString(), anyString());
    }

    @Test
    void naoGeraNovoTokenQuandoProtocoloJaTemUm() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setStatus(StatusProtocolo.AGENDADO);
        protocolo.setConfirmacaoToken("token-existente");
        when(protocoloRepository.findParaLembreteAgendamento(eq(StatusProtocolo.AGENDADO), any(LocalDate.class)))
                .thenReturn(List.of(protocolo));
        when(whatsappCredenciaisService.ativas()).thenReturn(Optional.empty());

        processor(1).processarTenantCorrente("prefeitura-x");

        assertThat(protocolo.getConfirmacaoToken()).isEqualTo("token-existente");
    }

    @Test
    void disparaWhatsappApenasQuandoTenantTemCredenciaisAtivas() {
        Protocolo protocolo = protocolo(10L, paciente, procedimento);
        protocolo.setStatus(StatusProtocolo.AGENDADO);
        when(protocoloRepository.findParaLembreteAgendamento(eq(StatusProtocolo.AGENDADO), any(LocalDate.class)))
                .thenReturn(List.of(protocolo));
        WhatsappCredenciaisService.Credenciais credenciais =
                new WhatsappCredenciaisService.Credenciais("123456", "token-de-acesso");
        when(whatsappCredenciaisService.ativas()).thenReturn(Optional.of(credenciais));

        processor(1).processarTenantCorrente("prefeitura-x");

        verify(notificacaoWhatsappService, times(1))
                .notificarLembreteAgendamento(protocolo, "prefeitura-x", "123456", "token-de-acesso");
    }
}
