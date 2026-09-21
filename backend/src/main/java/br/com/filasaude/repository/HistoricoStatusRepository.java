package br.com.filasaude.repository;

import br.com.filasaude.domain.HistoricoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {
    List<HistoricoStatus> findByProtocoloIdOrderByCriadoEmAsc(Long protocoloId);
}
