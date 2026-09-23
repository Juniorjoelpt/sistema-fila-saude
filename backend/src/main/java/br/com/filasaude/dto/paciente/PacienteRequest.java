package br.com.filasaude.dto.paciente;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record PacienteRequest(
        @NotBlank String nome,
        String cpf,
        String cns,
        LocalDate dataNascimento,
        String telefone,
        String email,
        Long acsResponsavelId,
        boolean pcd,
        boolean gestante
) {
}
