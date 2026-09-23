package br.com.filasaude.service;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Notificação ao cidadão por e-mail em mudança de status (itens 3.1 e 6.1 do
 * levantamento de requisitos: canal definido como e-mail + app web, incluído
 * já na fase 1/MVP). Envio assíncrono para não travar a resposta da API caso
 * o servidor SMTP esteja lento.
 *
 * Se o paciente não tiver e-mail cadastrado, ou o e-mail falhar, a operação
 * principal (mudança de status) não é revertida — a notificação é "best effort".
 */
@Service
public class NotificacaoEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoEmailService.class);

    private final JavaMailSender mailSender;
    private final String remetente;

    public NotificacaoEmailService(JavaMailSender mailSender,
                                    @Value("${filasaude.mail.remetente:naoresponda@filasaude.com.br}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Async
    public void notificarMudancaStatus(Protocolo protocolo) {
        String destinatario = protocolo.getPaciente().getEmail();
        if (destinatario == null || destinatario.isBlank()) {
            log.debug("Paciente do protocolo {} não possui e-mail cadastrado; notificação não enviada",
                    protocolo.getNumeroProtocolo());
            return;
        }

        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente);
            mensagem.setTo(destinatario);
            mensagem.setSubject("Atualização do seu protocolo " + protocolo.getNumeroProtocolo());
            mensagem.setText(corpoMensagem(protocolo));
            mailSender.send(mensagem);
            log.info("Notificação enviada para o protocolo {}", protocolo.getNumeroProtocolo());
        } catch (Exception e) {
            log.warn("Falha ao enviar notificação por e-mail do protocolo {}: {}",
                    protocolo.getNumeroProtocolo(), e.getMessage());
        }
    }

    /**
     * Alerta de SLA vencido (protocolos com prazo "Atrasado" ainda não notificados --
     * ver {@code SlaAlertaService}). Um único e-mail-resumo por destinatário, para não
     * inundar a caixa do regulador/admin com um e-mail por protocolo em dias de pico.
     */
    @Async
    public void notificarProtocolosAtrasados(List<Protocolo> protocolosAtrasados, List<Usuario> destinatarios) {
        if (protocolosAtrasados.isEmpty() || destinatarios.isEmpty()) {
            return;
        }

        String corpo = corpoResumoAtrasados(protocolosAtrasados);
        String assunto = "Fila Saúde — %d protocolo(s) com prazo de atendimento vencido"
                .formatted(protocolosAtrasados.size());

        for (Usuario destinatario : destinatarios) {
            try {
                SimpleMailMessage mensagem = new SimpleMailMessage();
                mensagem.setFrom(remetente);
                mensagem.setTo(destinatario.getEmail());
                mensagem.setSubject(assunto);
                mensagem.setText(corpo);
                mailSender.send(mensagem);
            } catch (Exception e) {
                log.warn("Falha ao enviar alerta de SLA para {}: {}", destinatario.getEmail(), e.getMessage());
            }
        }
        log.info("Alerta de SLA enviado para {} destinatário(s) sobre {} protocolo(s) atrasado(s)",
                destinatarios.size(), protocolosAtrasados.size());
    }

    private String corpoResumoAtrasados(List<Protocolo> protocolosAtrasados) {
        String linhas = protocolosAtrasados.stream()
                .map(p -> "- %s | %s | %s | %d dias em espera".formatted(
                        p.getNumeroProtocolo(),
                        p.getPaciente().getNome(),
                        p.getProcedimento().getNome(),
                        p.diasEmEspera()))
                .collect(Collectors.joining("\n"));

        return """
               Olá!

               Os protocolos abaixo estão com o prazo de atendimento vencido (mais de 15 dias
               aguardando na fila) e ainda aguardam regulação:

               %s

               Acesse o painel administrativo para regularizar a situação.

               Esta é uma mensagem automática, por favor não responda a este e-mail.
               """.formatted(linhas);
    }

    private String corpoMensagem(Protocolo protocolo) {
        return """
               Olá, %s!

               O status do seu protocolo %s (%s) foi atualizado para: %s

               Você pode acompanhar todos os detalhes a qualquer momento consultando
               seu protocolo pelo CPF ou CNS no portal da Secretaria de Saúde.

               Esta é uma mensagem automática, por favor não responda a este e-mail.
               """.formatted(
                protocolo.getPaciente().getNome(),
                protocolo.getNumeroProtocolo(),
                protocolo.getProcedimento().getNome(),
                protocolo.getStatus().name()
        );
    }
}
