package br.com.filasaude.repository;

import br.com.filasaude.domain.HistoricoPrioridade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoPrioridadeRepository extends JpaRepository<HistoricoPrioridade, Long> {
    List<HistoricoPrioridade> findByProtocoloIdOrderByCriadoEmAsc(Long protocoloId);
}
