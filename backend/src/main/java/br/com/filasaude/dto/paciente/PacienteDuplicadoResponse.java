package br.com.filasaude.dto.paciente;

import java.time.LocalDate;

/**
 * Um paciente já cadastrado que PODE ser o mesmo que está sendo digitado
 * agora (item do roadmap "detecção de duplicidade fuzzy de paciente").
 * "pontuacaoSimilaridade" é a semelhança do nome (0-100); "motivo" lista o
 * que corroborou o alerta (nome semelhante sozinho só avisa com uma barra
 * bem alta -- ver DuplicidadePacienteService).
 */
public record PacienteDuplicadoResponse(
        Long pacienteId,
        String nome,
        String cpf,
        String cns,
        LocalDate dataNascimento,
        String telefone,
        int pontuacaoSimilaridade,
        String motivo
) {
}
