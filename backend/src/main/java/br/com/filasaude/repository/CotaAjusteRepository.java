package br.com.filasaude.repository;

import br.com.filasaude.domain.CotaAjuste;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CotaAjusteRepository extends JpaRepository<CotaAjuste, Long> {
    List<CotaAjuste> findByCotaIdOrderByCriadoEmDesc(Long cotaId);
}
