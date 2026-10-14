package br.com.filasaude.repository;

import br.com.filasaude.domain.HistoricoStatus;
import br.com.filasaude.domain.enums.StatusProtocolo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {
    List<HistoricoStatus> findByProtocoloIdOrderByCriadoEmAsc(Long protocoloId);

    /**
     * Saídas da fila por procedimento desde uma data (linhas [procedimentoId, quantidade]):
     * protocolos distintos que mudaram de {@code anterior} para um dos {@code novos} (remarcações não contam em dobro) -- base da vazão usada no
     * dashboard preditivo de SLA (ver PrevisaoSlaService).
     */
    @Query("""
           SELECT h.protocolo.procedimento.id, COUNT(DISTINCT h.protocolo.id) FROM HistoricoStatus h
           WHERE h.statusAnterior = :anterior AND h.statusNovo IN :novos AND h.criadoEm >= :desde
           GROUP BY h.protocolo.procedimento.id
           """)
    List<Object[]> contarSaidasDaFila(@Param("anterior") StatusProtocolo anterior,
                                       @Param("novos") Collection<StatusProtocolo> novos,
                                       @Param("desde") LocalDateTime desde);
}
