package br.com.filasaude.repository;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.StatusProtocolo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProtocoloRepository extends JpaRepository<Protocolo, Long>, JpaSpecificationExecutor<Protocolo> {

    Optional<Protocolo> findByNumeroProtocolo(String numeroProtocolo);

    @Query("""
           SELECT p FROM Protocolo p
           JOIN FETCH p.paciente pac
           WHERE (pac.cpf = :documento OR pac.cns = :documento)
           ORDER BY p.criadoEm DESC
           """)
    List<Protocolo> findByDocumentoPaciente(@Param("documento") String documento);

    long countByStatus(StatusProtocolo status);

    /**
     * Todos os protocolos aguardando atendimento. A ordenacao pelo motor de
     * priorizacao (categoria + FIFO por data) e feita em memoria pelo
     * {@code FilaPriorizacaoService}, mais simples e legivel do que replicar
     * a logica de peso das categorias em SQL.
     */
    List<Protocolo> findByStatus(StatusProtocolo status);
}
