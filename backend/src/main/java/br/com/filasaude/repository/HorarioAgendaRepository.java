package br.com.filasaude.repository;

import br.com.filasaude.domain.HorarioAgenda;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface HorarioAgendaRepository extends JpaRepository<HorarioAgenda, Long> {

    List<HorarioAgenda> findByUnidadeSaudeIdAndDataBetweenOrderByDataAscHoraInicioAsc(
            Long unidadeSaudeId, LocalDate inicio, LocalDate fim);

    List<HorarioAgenda> findByUnidadeSaudeIdAndEspecialidadeAndDataBetweenOrderByDataAscHoraInicioAsc(
            Long unidadeSaudeId, String especialidade, LocalDate inicio, LocalDate fim);

    Optional<HorarioAgenda> findByUnidadeSaudeIdAndEspecialidadeAndDataAndHoraInicio(
            Long unidadeSaudeId, String especialidade, LocalDate data, LocalTime horaInicio);

    /**
     * Bug crítico de revisão corrigido: {@code agendarHorario} fazia um
     * SELECT COUNT de vagas ocupadas e só depois gravava o vínculo, sem
     * nenhum lock -- duas requisições concorrentes para o mesmo horário
     * liam "0 ocupadas" antes de qualquer commit e ambas passavam na
     * validação, causando overbooking (2 protocolos para 1 vaga). Este
     * método busca a linha com {@code SELECT ... FOR UPDATE}, serializando
     * qualquer outra transação que tente agendar/consultar a mesma linha
     * com lock até a transação atual commitar ou dar rollback.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HorarioAgenda h where h.id = :id")
    Optional<HorarioAgenda> buscarComLockParaAgendamento(@Param("id") Long id);

    /** Vagas ofertadas por unidade num período (transparência ativa). Linhas [nome da unidade, soma da capacidade]. */
    @Query("""
           SELECT h.unidadeSaude.nome, SUM(h.capacidadeTotal) FROM HorarioAgenda h
           WHERE h.data BETWEEN :inicio AND :fim
           GROUP BY h.unidadeSaude.nome ORDER BY h.unidadeSaude.nome
           """)
    List<Object[]> somarVagasPorUnidade(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
