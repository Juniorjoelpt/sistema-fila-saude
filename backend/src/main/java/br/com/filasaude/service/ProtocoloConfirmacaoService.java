package br.com.filasaude.service;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.PresencaConfirmacao;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.publico.ConfirmacaoPresencaResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.ProtocoloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * Confirmação/cancelamento de presença pelo cidadão, a partir do link
 * público enviado no lembrete de agendamento (ver
 * LembreteAgendamentoService/NotificacaoEmailService). Identidade provada
 * apenas pela posse do token (mesmo padrão de outras rotas públicas deste
 * sistema, que provam posse do CPF/CNS) -- sem exigir login nem CPF/CNS de
 * novo, para que o clique no e-mail resolva tudo em um passo.
 */
@Service
@Transactional
public class ProtocoloConfirmacaoService {

    /**
     * Bug crítico de revisão corrigido: confirmar()/cancelar() não checavam
     * o status do protocolo nem o valor atual de presencaConfirmacao, então
     * o link (sem autenticação nenhuma, só a posse do token) podia ser
     * reutilizado indefinidamente -- inclusive dias depois do atendimento já
     * concluído ou do protocolo ter sido cancelado por outro motivo. Só
     * protocolos ainda "em curso" (aguardando o atendimento acontecer) podem
     * ter a presença confirmada/cancelada por aqui.
     */
    private static final Set<StatusProtocolo> STATUS_PERMITE_CONFIRMACAO =
            EnumSet.of(StatusProtocolo.AGENDADO, StatusProtocolo.EM_ANDAMENTO);

    private final ProtocoloRepository protocoloRepository;
    private final AuditoriaService auditoriaService;

    public ProtocoloConfirmacaoService(ProtocoloRepository protocoloRepository,
                                        AuditoriaService auditoriaService) {
        this.protocoloRepository = protocoloRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public ConfirmacaoPresencaResponse consultar(String token) {
        return toResponse(buscarPorToken(token));
    }

    public ConfirmacaoPresencaResponse confirmar(String token) {
        Protocolo protocolo = buscarPorToken(token);
        validarPodeResponder(protocolo);
        protocolo.setPresencaConfirmacao(PresencaConfirmacao.CONFIRMADA);
        protocoloRepository.save(protocolo);
        auditoriaService.registrar("CONFIRMAR_PRESENCA", "Protocolo", protocolo.getId(),
                "Presença confirmada pelo paciente via link de lembrete");
        return toResponse(protocolo);
    }

    public ConfirmacaoPresencaResponse cancelar(String token) {
        Protocolo protocolo = buscarPorToken(token);
        validarPodeResponder(protocolo);
        protocolo.setPresencaConfirmacao(PresencaConfirmacao.CANCELADA);
        protocoloRepository.save(protocolo);
        auditoriaService.registrar("CANCELAR_PRESENCA", "Protocolo", protocolo.getId(),
                "Presença cancelada pelo paciente via link de lembrete");
        return toResponse(protocolo);
    }

    private void validarPodeResponder(Protocolo protocolo) {
        if (!STATUS_PERMITE_CONFIRMACAO.contains(protocolo.getStatus())) {
            throw new IllegalStateException(
                    "Este atendimento não está mais aguardando confirmação de presença (status atual: "
                            + protocolo.getStatus() + ").");
        }
        if (protocolo.getDataPrevista() != null && protocolo.getDataPrevista().isBefore(LocalDate.now())) {
            throw new IllegalStateException("A data deste atendimento já passou.");
        }
    }

    private Protocolo buscarPorToken(String token) {
        return protocoloRepository.findByConfirmacaoToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Link de confirmação inválido ou expirado"));
    }

    private ConfirmacaoPresencaResponse toResponse(Protocolo protocolo) {
        return new ConfirmacaoPresencaResponse(
                protocolo.getNumeroProtocolo(),
                protocolo.getPaciente().getNome(),
                protocolo.getProcedimento().getNome(),
                protocolo.getHorarioAgendado() != null && protocolo.getHorarioAgendado().getUnidadeSaude() != null
                        ? protocolo.getHorarioAgendado().getUnidadeSaude().getNome()
                        : null,
                protocolo.getDataPrevista(),
                protocolo.getHorarioAgendado() != null ? protocolo.getHorarioAgendado().getHoraInicio() : null,
                protocolo.getPresencaConfirmacao() != null ? protocolo.getPresencaConfirmacao().name() : null
        );
    }
}
