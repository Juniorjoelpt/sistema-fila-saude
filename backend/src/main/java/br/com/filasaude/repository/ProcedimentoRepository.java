package br.com.filasaude.repository;

import br.com.filasaude.domain.Procedimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, Long> {
    List<Procedimento> findByAtivoTrue();

    Optional<Procedimento> findByNomeIgnoreCase(String nome);
}
