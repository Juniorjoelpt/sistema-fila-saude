package br.com.filasaude.repository;

import br.com.filasaude.domain.UnidadeSaude;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeSaudeRepository extends JpaRepository<UnidadeSaude, Long> {
}
