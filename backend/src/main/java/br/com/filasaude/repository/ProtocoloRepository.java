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

    /**
     * Protocolos de uma unidade+especialidade num mes de referencia -- usado
     * para calcular o percentual de ocupacao de uma cota (item 3.3). O
     * filtro de status CANCELADO fica no service, para nao acoplar a query a
     * enum literal em JPQL.
     */
    List<Protocolo> findByUnidadeSaudeIdAndProcedimento_EspecialidadeAndDataInclusaoBetween(
            Long unidadeSaudeId, String especialidade, java.time.LocalDate inicio, java.time.LocalDate fim);

    /**
     * Protocolos ainda aguardando atendimento cuja data de inclusão já ultrapassou o
     * limite de dias considerado "Atrasado" (mesmo critério do badge de prazo da fila,
     * ver {@code nivelPrazo} no frontend) e que ainda não geraram alerta de SLA --
     * usado pela varredura diária do {@code SlaAlertaService}.
     */
    List<Protocolo> findByStatusAndDataInclusaoLessThanEqualAndAlertaSlaEnviadoEmIsNull(
            StatusProtocolo status, java.time.LocalDate dataLimite);
}
