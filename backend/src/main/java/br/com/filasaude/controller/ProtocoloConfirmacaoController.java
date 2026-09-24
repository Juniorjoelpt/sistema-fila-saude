package br.com.filasaude.controller;

import br.com.filasaude.dto.publico.ConfirmacaoPresencaResponse;
import br.com.filasaude.service.ProtocoloConfirmacaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rota pública (sem autenticação) de confirmação de presença, acessada pelo
 * link enviado no lembrete de agendamento (ver
 * LembreteAgendamentoService/NotificacaoEmailService). Identidade provada
 * apenas pela posse do token -- mesmo espírito de /api/public/protocolo
 * (ProtocoloPublicoController), mas sem exigir CPF/CNS, já que o token já é
 * o "segredo" entregue por e-mail.
 */
@RestController
@RequestMapping("/api/public/confirmacao")
public class ProtocoloConfirmacaoController {

    private final ProtocoloConfirmacaoService protocoloConfirmacaoService;

    public ProtocoloConfirmacaoController(ProtocoloConfirmacaoService protocoloConfirmacaoService) {
        this.protocoloConfirmacaoService = protocoloConfirmacaoService;
    }

    @GetMapping("/{token}")
    public ConfirmacaoPresencaResponse consultar(@PathVariable String token) {
        return protocoloConfirmacaoService.consultar(token);
    }

    @PostMapping("/{token}/confirmar")
    public ConfirmacaoPresencaResponse confirmar(@PathVariable String token) {
        return protocoloConfirmacaoService.confirmar(token);
    }

    @PostMapping("/{token}/cancelar")
    public ConfirmacaoPresencaResponse cancelar(@PathVariable String token) {
        return protocoloConfirmacaoService.cancelar(token);
    }
}
