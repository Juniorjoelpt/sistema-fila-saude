package br.com.filasaude.dto.paciente;

import br.com.filasaude.domain.Paciente;

import java.time.LocalDate;

public record PacienteResponse(
        Long id,
        String nome,
        String cpf,
        String cns,
        LocalDate dataNascimento,
        String telefone,
        String email,
        Long acsResponsavelId,
        String acsResponsavelNome,
        boolean pcd,
        boolean gestante
) {
    public static PacienteResponse de(Paciente p) {
        return new PacienteResponse(
                p.getId(), p.getNome(), p.getCpf(), p.getCns(), p.getDataNascimento(),
                p.getTelefone(), p.getEmail(),
                p.getAcsResponsavel() != null ? p.getAcsResponsavel().getId() : null,
                p.getAcsResponsavel() != null ? p.getAcsResponsavel().getNome() : null,
                p.isPcd(), p.isGestante()
        );
    }
}
