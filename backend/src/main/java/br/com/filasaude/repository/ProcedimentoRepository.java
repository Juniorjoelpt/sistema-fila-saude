package br.com.filasaude.repository;

import br.com.filasaude.domain.Procedimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, Long> {
    List<Procedimento> findByAtivoTrue();
}
