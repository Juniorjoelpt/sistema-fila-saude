package br.com.filasaude.service;

import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.domain.enums.Papel;
import br.com.filasaude.dto.paciente.PacienteRequest;
import br.com.filasaude.dto.paciente.PacienteResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.PacienteRepository;
import br.com.filasaude.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public PacienteService(PacienteRepository pacienteRepository, UsuarioRepository usuarioRepository,
                            AuditoriaService auditoriaService) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    public PacienteResponse cadastrar(PacienteRequest request) {
    // Bug de revisão corrigido: o banco tem uma constraint (chk_paciente_documento)
    // exigindo CPF ou CNS, mas não havia validação equivalente na aplicação --
    // submeter sem os dois campos estourava a constraint no INSERT e virava um
    // "erro inesperado" (500) genérico para o operador, sem dizer o que corrigir.
    // Validando aqui antes, o erro fica claro e específico (400).
    if (somenteDigitosOuNulo(request.cpf()) == null && somenteDigitosOuNulo(request.cns()) == null) {
        throw new IllegalStateException("Informe ao menos um documento do paciente: CPF ou CNS.");
    }

    Optional<Usuario> usuarioLogado = usuarioLogado();

        // ACS (item 2 do levantamento de requisitos: "cadastra e acompanha
        // pacientes da sua área") sempre vira o responsável automaticamente --
        // não confiamos em um acsResponsavelId vindo do cliente para esse papel,
        // para o vínculo não poder ser forjado para outro ACS.
        Usuario acs;
        if (usuarioLogado.isPresent() && usuarioLogado.get().getPapel() == Papel.ACS) {
            acs = usuarioLogado.get();
        } else if (request.acsResponsavelId() != null) {
            acs = usuarioRepository.findById(request.acsResponsavelId())
                    .orElseThrow(() -> new ResourceNotFoundException("ACS não encontrado: " + request.acsResponsavelId()));
        } else {
            acs = null;
        }

        Paciente paciente = Paciente.builder()
                .nome(request.nome())
                .cpf(somenteDigitosOuNulo(request.cpf()))
                .cns(somenteDigitosOuNulo(request.cns()))
                .dataNascimento(request.dataNascimento())
                .telefone(request.telefone())
                .email(request.email())
                .acsResponsavel(acs)
                .pcd(request.pcd())
                .gestante(request.gestante())
                .build();

        Paciente salvo = pacienteRepository.save(paciente);
        auditoriaService.registrar("CRIAR_PACIENTE", "Paciente", salvo.getId(), "Cadastrado " + salvo.getNome());
        return PacienteResponse.de(salvo);
    }

    /**
     * Listagem de pacientes (item 3.4). Quando o chamador é ACS, a listagem
     * é automaticamente restrita aos pacientes da sua área -- o ACS não deve
     * ver a base completa de pacientes do município.
     */
    @Transactional(readOnly = true)
    public List<PacienteResponse> listar() {
        Optional<Usuario> usuarioLogado = usuarioLogado();
        if (usuarioLogado.isPresent() && usuarioLogado.get().getPapel() == Papel.ACS) {
            return pacienteRepository.findByAcsResponsavelIdOrderByNomeAsc(usuarioLogado.get().getId()).stream()
                    .map(PacienteResponse::de).toList();
        }
        return pacienteRepository.findAll().stream().map(PacienteResponse::de).toList();
    }

    /**
     * Busca por ID (item 3.4). Diferente de {@link #listar()}, este endpoint não
     * tinha nenhum filtro por ACS responsável -- um ACS autenticado conseguia ler
     * dados de qualquer paciente do tenant (CPF, CNS, telefone, e-mail) só
     * incrementando o ID na URL. Corrigido aplicando o mesmo filtro da listagem,
     * tratando como "não encontrado" em vez de "acesso negado" para não confirmar
     * a existência do paciente a quem não deveria vê-lo (mesmo critério usado em
     * {@code ProtocoloService.buscarDetalhe}).
     */
    @Transactional(readOnly = true)
    public PacienteResponse buscarPorId(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + id));

        Optional<Usuario> usuarioLogado = usuarioLogado();
        if (usuarioLogado.isPresent() && usuarioLogado.get().getPapel() == Papel.ACS) {
            Usuario acsResponsavel = paciente.getAcsResponsavel();
            if (acsResponsavel == null || !acsResponsavel.getId().equals(usuarioLogado.get().getId())) {
                throw new ResourceNotFoundException("Paciente não encontrado: " + id);
            }
        }

        return PacienteResponse.de(paciente);
    }

    /**
     * Reatribui o ACS responsável (gap de revisão corrigido: antes não existia
     * NENHUMA forma de editar esse vínculo depois do cadastro -- um paciente
     * criado por Regulador/Admin sem ACS escolhido ficava "órfão" para sempre).
     * Só acessível a Regulador/Admin (ver SecurityConfig); aceita ACS nulo para
     * desfazer o vínculo.
     */
    public PacienteResponse atribuirAcsResponsavel(Long pacienteId, Long acsResponsavelId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + pacienteId));

        Usuario acs = null;
        if (acsResponsavelId != null) {
            acs = usuarioRepository.findById(acsResponsavelId)
                    .filter(u -> u.getPapel() == Papel.ACS)
                    .orElseThrow(() -> new ResourceNotFoundException("ACS não encontrado: " + acsResponsavelId));
        }

        paciente.setAcsResponsavel(acs);
        Paciente salvo = pacienteRepository.save(paciente);
        auditoriaService.registrar("REATRIBUIR_ACS_PACIENTE", "Paciente", salvo.getId(),
                "Paciente " + salvo.getNome() + " -> ACS responsável: "
                        + (acs != null ? acs.getNome() : "(nenhum)"));

        return PacienteResponse.de(salvo);
    }

    private String somenteDigitosOuNulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.replaceAll("\\D", "");
    }

    private Optional<Usuario> usuarioLogado() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .flatMap(usuarioRepository::findByEmailIgnoreCase);
    }
}
