package br.com.filasaude.repository;

import br.com.filasaude.domain.IntegracaoConfig;
import br.com.filasaude.domain.enums.TipoIntegracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntegracaoConfigRepository extends JpaRepository<IntegracaoConfig, Long> {
    Optional<IntegracaoConfig> findByTipo(TipoIntegracao tipo);
}
