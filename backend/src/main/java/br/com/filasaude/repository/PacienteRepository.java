package br.com.filasaude.repository;

import br.com.filasaude.domain.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    Optional<Paciente> findByCpf(String cpf);
    Optional<Paciente> findByCns(String cns);
    List<Paciente> findByAcsResponsavelIdOrderByNomeAsc(Long acsResponsavelId);
}
