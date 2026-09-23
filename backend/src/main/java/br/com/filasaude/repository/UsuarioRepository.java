package br.com.filasaude.repository;

import br.com.filasaude.domain.Usuario;
import br.com.filasaude.domain.enums.Papel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCaseAndAtivoTrue(String email);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findByPapelOrderByNomeAsc(Papel papel);

    List<Usuario> findAllByOrderByNomeAsc();
}
