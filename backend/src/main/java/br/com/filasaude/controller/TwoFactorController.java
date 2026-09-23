package br.com.filasaude.controller;

import br.com.filasaude.dto.twofactor.TwoFactorConfirmarRequest;
import br.com.filasaude.dto.twofactor.TwoFactorDesabilitarRequest;
import br.com.filasaude.dto.twofactor.TwoFactorSetupResponse;
import br.com.filasaude.dto.twofactor.TwoFactorStatusResponse;
import br.com.filasaude.service.TwoFactorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autoatendimento de 2FA da própria conta -- fora de /api/usuarios/** de
 * propósito (lá, escrita é restrita a Admin; aqui, qualquer papel autenticado
 * mexe na própria conta, e só na própria: TwoFactorService nunca recebe um
 * id de usuário vindo do cliente).
 */
@RestController
@RequestMapping("/api/me/2fa")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    public TwoFactorController(TwoFactorService twoFactorService) {
        this.twoFactorService = twoFactorService;
    }

    @GetMapping
    public TwoFactorStatusResponse status() {
        return twoFactorService.status();
    }

    @PostMapping("/iniciar")
    public TwoFactorSetupResponse iniciar() {
        return twoFactorService.iniciarConfiguracao();
    }

    @PostMapping("/confirmar")
    public TwoFactorStatusResponse confirmar(@Valid @RequestBody TwoFactorConfirmarRequest request) {
        twoFactorService.confirmar(request.codigo());
        return twoFactorService.status();
    }

    @PostMapping("/desabilitar")
    public TwoFactorStatusResponse desabilitar(@Valid @RequestBody TwoFactorDesabilitarRequest request) {
        twoFactorService.desabilitar(request.senha());
        return twoFactorService.status();
    }
}
