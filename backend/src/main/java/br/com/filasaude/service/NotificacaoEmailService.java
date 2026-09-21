package br.com.filasaude.service;

import br.com.filasaude.domain.Protocolo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
