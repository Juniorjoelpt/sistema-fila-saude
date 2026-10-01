package br.com.filasaude.repository;

import br.com.filasaude.domain.SlaConfiguracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SlaConfiguracaoRepository extends JpaRepository<SlaConfiguracao, Long> {

    List<SlaConfiguracao> findAllByOrderByEspecialidadeAscProcedimentoIdAsc();

    Optional<SlaConfiguracao> findByEspecialidade(String especialidade);

    Optional<SlaConfiguracao> findByProcedimentoId(Long procedimentoId);

    boolean existsByEspecialidadeAndIdNot(String especialidade, Long id);

    boolean existsByProcedimentoIdAndIdNot(Long procedimentoId, Long id);
}
