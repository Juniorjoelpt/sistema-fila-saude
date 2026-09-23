package br.com.filasaude.controller;

import br.com.filasaude.domain.enums.Papel;
import br.com.filasaude.dto.usuario.UsuarioCreateRequest;
import br.com.filasaude.dto.usuario.UsuarioResponse;
import br.com.filasaude.dto.usuario.UsuarioUpdateRequest;
import br.com.filasaude.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestão de equipe (item 3.4): cadastro de operadores/reguladores/ACS com
 * níveis de acesso por papel. Leitura liberada para ACS/Regulador/Admin (ex.:
 * preencher o filtro "ACS responsável" na fila); escrita restrita a Admin
 * (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) Papel papel) {
        return usuarioService.listar(papel);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioCreateRequest request) {
        return usuarioService.criar(request);
    }

    @PatchMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return usuarioService.atualizar(id, request);
    }
}
