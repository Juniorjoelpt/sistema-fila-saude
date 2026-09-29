package br.com.filasaude.repository;

import br.com.filasaude.domain.UnidadeSaude;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnidadeSaudeRepository extends JpaRepository<UnidadeSaude, Long> {

    Optional<UnidadeSaude> findByNomeIgnoreCase(String nome);

    Optional<UnidadeSaude> findByCodigoCnes(String codigoCnes);
}
