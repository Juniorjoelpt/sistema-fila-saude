package br.com.filasaude.repository;

import br.com.filasaude.domain.HorarioAgenda;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
