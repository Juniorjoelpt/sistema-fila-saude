package br.com.filasaude.repository;

import br.com.filasaude.domain.Especialidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EspecialidadeRepository extends JpaRepository<Especialidade, Long> {

    List<Especialidade> findByAtivoTrueOrderByNomeAsc();

    Optional<Especialidade> findByNomeIgnoreCase(String nome);
}
