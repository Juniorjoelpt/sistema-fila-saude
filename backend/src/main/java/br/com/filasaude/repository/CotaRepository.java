package br.com.filasaude.repository;

import br.com.filasaude.domain.Cota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CotaRepository extends JpaRepository<Cota, Long> {

    List<Cota> findByMesReferenciaOrderByUnidadeSaude_NomeAscEspecialidadeAsc(LocalDate mesReferencia);

    List<Cota> findByUnidadeSaudeIdAndMesReferenciaOrderByEspecialidadeAsc(Long unidadeSaudeId, LocalDate mesReferencia);

    Optional<Cota> findByUnidadeSaudeIdAndEspecialidadeAndMesReferencia(
            Long unidadeSaudeId, String especialidade, LocalDate mesReferencia);
}
