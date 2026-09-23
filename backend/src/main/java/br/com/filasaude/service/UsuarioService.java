package br.com.filasaude.service;

import br.com.filasaude.domain.Usuario;
import br.com.filasaude.domain.enums.Papel;
import br.com.filasaude.dto.usuario.UsuarioCreateRequest;
import br.com.filasaude.dto.usuario.UsuarioResponse;
import br.com.filasaude.dto.usuario.UsuarioUpdateRequest;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestão de equipe (item 3.4 do levantamento de requisitos): cadastro de
 * operadores/reguladores/ACS com nível de acesso por papel (RBAC). Escopo por
 * tenant — cada prefeitura gerencia sua própria equipe.
 */
@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                           AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Papel papel) {
        List<Usuario> usuarios = papel != null
                ? usuarioRepository.findByPapelOrderByNomeAsc(papel)
                : usuarioRepository.findAllByOrderByNomeAsc();
        return usuarios.stream().map(UsuarioResponse::de).toList();
    }

    public UsuarioResponse criar(UsuarioCreateRequest request) {
        usuarioRepository.findByEmailIgnoreCase(request.email()).ifPresent(u -> {
            throw new IllegalStateException("Já existe um usuário cadastrado com este e-mail");
        });

        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .email(request.email())
                .senhaHash(passwordEncoder.encode(request.senha()))
                .papel(request.papel())
                .ativo(true)
                .build();

        Usuario salvo = usuarioRepository.save(usuario);
        auditoriaService.registrar("CRIAR_USUARIO", "Usuario", salvo.getId(),
                "Criado " + salvo.getEmail() + " (papel " + salvo.getPapel() + ")");
        return UsuarioResponse.de(salvo);
    }

    public UsuarioResponse atualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));

        usuario.setNome(request.nome());
        usuario.setPapel(request.papel());
        usuario.setAtivo(request.ativo());

        if (request.novaSenha() != null && !request.novaSenha().isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        }

        Usuario salvo = usuarioRepository.save(usuario);
        auditoriaService.registrar("EDITAR_USUARIO", "Usuario", salvo.getId(),
                "Editado " + salvo.getEmail() + " (papel " + salvo.getPapel() + ", ativo=" + salvo.isAtivo() + ")");
        return UsuarioResponse.de(salvo);
    }
}
