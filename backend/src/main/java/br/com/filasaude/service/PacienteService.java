package br.com.filasaude.service;

import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.dto.paciente.PacienteRequest;
import br.com.filasaude.dto.paciente.PacienteResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.PacienteRepository;
import br.com.filasaude.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;

    public PacienteService(PacienteRepository pacienteRepository, UsuarioRepository usuarioRepository) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public PacienteResponse cadastrar(PacienteRequest request) {
        Usuario acs = null;
        if (request.acsResponsavelId() != null) {
            acs = usuarioRepository.findById(request.acsResponsavelId())
                    .orElseThrow(() -> new ResourceNotFoundException("ACS não encontrado: " + request.acsResponsavelId()));
        }

        Paciente paciente = Paciente.builder()
                .nome(request.nome())
                .cpf(somenteDigitosOuNulo(request.cpf()))
                .cns(somenteDigitosOuNulo(request.cns()))
                .dataNascimento(request.dataNascimento())
                .telefone(request.telefone())
                .email(request.email())
                .acsResponsavel(acs)
                .build();

        return PacienteResponse.de(pacienteRepository.save(paciente));
    }

    @Transactional(readOnly = true)
    public List<PacienteResponse> listar() {
        return pacienteRepository.findAll().stream().map(PacienteResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PacienteResponse buscarPorId(Long id) {
        return pacienteRepository.findById(id)
                .map(PacienteResponse::de)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + id));
    }

    private String somenteDigitosOuNulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.replaceAll("\\D", "");
    }
}
