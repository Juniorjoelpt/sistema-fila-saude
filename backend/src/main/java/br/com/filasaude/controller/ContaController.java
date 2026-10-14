package br.com.filasaude.controller;

import br.com.filasaude.dto.auth.AlterarSenhaRequest;
import br.com.filasaude.dto.auth.LoginResponse;
import br.com.filasaude.service.ContaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autoatendimento da própria conta (qualquer papel autenticado), fora de
 * /api/usuarios/** de propósito -- lá, escrita é restrita a Admin. Esta é a
 * única rota liberada a quem ainda usa senha provisória (ver
 * JwtAuthenticationFilter).
 */
@RestController
@RequestMapping("/api/me")
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @PostMapping("/senha")
    public LoginResponse alterarSenha(@Valid @RequestBody AlterarSenhaRequest request) {
        return contaService.alterarSenha(request.senhaAtual(), request.novaSenha());
    }
}
