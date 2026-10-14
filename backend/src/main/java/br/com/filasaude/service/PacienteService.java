package br.com.filasaude.service;

import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.domain.enums.Papel;
import br.com.filasaude.dto.paciente.PacienteRequest;
import br.com.filasaude.dto.paciente.PacienteResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.PacienteRepository;
import br.com.filasaude.repository.ProtocoloRepository;
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
    private final ProtocoloRepository protocoloRepository;
    private final AuditoriaService auditoriaService;

    public PacienteService(PacienteRepository pacienteRepository, UsuarioRepository usuarioRepository,
                            ProtocoloRepository protocoloRepository, AuditoriaService auditoriaService) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.protocoloRepository = protocoloRepository;
        this.auditoriaService = auditoriaService;
    }

    public PacienteResponse cadastrar(PacienteRequest request) {
    // Bug de revisão corrigido: o banco tem uma constraint (chk_paciente_documento)
    // exigindo CPF ou CNS, mas não havia validação equivalente na aplicação --
    // submeter sem os dois campos estourava a constraint no INSERT e virava um
    // "erro inesperado" (500) genérico para o operador, sem dizer o que corrigir.
    // Validando aqui antes, o erro fica claro e específico (400).
    String cpf = somenteDigitosOuNulo(request.cpf());
    String cns = somenteDigitosOuNulo(request.cns());
    if (cpf == null && cns == null) {
        throw new IllegalStateException("Informe ao menos um documento do paciente: CPF ou CNS.");
    }
    validarFormatoDocumento(cpf, cns, null, null);
    exigirDocumentoDisponivel(cpf, cns, null);
    // Sem data de nascimento o paciente não consegue consultar o próprio protocolo no portal
    // público (que exige CPF/CNS + data) nem ter a classificação por idade confirmada.
    if (request.dataNascimento() == null) {
        throw new IllegalStateException("Informe a data de nascimento do paciente.");
    }
    if (request.dataNascimento().isAfter(java.time.LocalDate.now())) {
        throw new IllegalStateException("A data de nascimento não pode estar no futuro.");
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
                .cpf(cpf)
                .cns(cns)
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

    /**
     * Bug alto de revisão corrigido: não existia NENHUM endpoint para editar
     * dados cadastrais do paciente (nome, telefone, CPF/CNS, etc.) depois do
     * cadastro -- só o vínculo de ACS podia ser alterado. Um erro de
     * digitação no cadastro (ex.: telefone errado) ficava permanente. Mesma
     * validação de documento e mesma restrição de visibilidade por ACS
     * aplicadas em {@link #cadastrar} e {@link #buscarPorId}.
     */
    public PacienteResponse atualizar(Long id, PacienteRequest request) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + id));

        Optional<Usuario> usuarioLogado = usuarioLogado();
        if (usuarioLogado.isPresent() && usuarioLogado.get().getPapel() == Papel.ACS) {
            Usuario acsResponsavel = paciente.getAcsResponsavel();
            if (acsResponsavel == null || !acsResponsavel.getId().equals(usuarioLogado.get().getId())) {
                throw new ResourceNotFoundException("Paciente não encontrado: " + id);
            }
        }

        String cpf = somenteDigitosOuNulo(request.cpf());
        String cns = somenteDigitosOuNulo(request.cns());
        if (cpf == null && cns == null) {
            throw new IllegalStateException("Informe ao menos um documento do paciente: CPF ou CNS.");
        }
        // Só confere os dígitos verificadores se o documento foi alterado: cadastros antigos
        // (anteriores a essa regra) continuam editáveis sem precisar corrigir o CPF/CNS.
        validarFormatoDocumento(cpf, cns, paciente.getCpf(), paciente.getCns());
        exigirDocumentoDisponivel(cpf, cns, paciente.getId());
        // Mesmas regras do cadastro: cadastros antigos sem data de nascimento precisam ser completados.
        if (request.dataNascimento() == null) {
            throw new IllegalStateException("Informe a data de nascimento do paciente.");
        }
        if (request.dataNascimento().isAfter(java.time.LocalDate.now())) {
            throw new IllegalStateException("A data de nascimento não pode estar no futuro.");
        }

        paciente.setNome(request.nome());
        paciente.setCpf(cpf);
        paciente.setCns(cns);
        paciente.setDataNascimento(request.dataNascimento());
        paciente.setTelefone(request.telefone());
        paciente.setEmail(request.email());
        paciente.setPcd(request.pcd());
        paciente.setGestante(request.gestante());

        Paciente salvo = pacienteRepository.save(paciente);
        auditoriaService.registrar("ATUALIZAR_PACIENTE", "Paciente", salvo.getId(), "Dados atualizados: " + salvo.getNome());
        return PacienteResponse.de(salvo);
    }

    /**
     * Bug alto de revisão corrigido: não havia forma de remover um cadastro
     * de paciente feito por engano (ex.: duplicado, teste). Bloqueia a
     * exclusão se já existir protocolo vinculado (histórico clínico não pode
     * sumir silenciosamente) em vez de deixar a constraint de FK estourar
     * como erro 500 genérico.
     */
    public void excluir(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + id));

        Optional<Usuario> usuarioLogado = usuarioLogado();
        if (usuarioLogado.isPresent() && usuarioLogado.get().getPapel() == Papel.ACS) {
            Usuario acsResponsavel = paciente.getAcsResponsavel();
            if (acsResponsavel == null || !acsResponsavel.getId().equals(usuarioLogado.get().getId())) {
                throw new ResourceNotFoundException("Paciente não encontrado: " + id);
            }
        }

        if (protocoloRepository.existsByPacienteId(id)) {
            throw new IllegalStateException(
                    "Não é possível excluir: este paciente possui protocolo(s) vinculado(s). "
                            + "Cancele ou finalize os protocolos antes de excluir o cadastro.");
        }

        pacienteRepository.delete(paciente);
        auditoriaService.registrar("EXCLUIR_PACIENTE", "Paciente", id, "Excluído: " + paciente.getNome());
    }

    private String somenteDigitosOuNulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.replaceAll("\\D", "");
    }

    /**
     * Bug alto de revisão corrigido: CPF/CNS só passavam por
     * {@link #somenteDigitosOuNulo}, que apenas remove caracteres não
     * numéricos -- qualquer sequência de dígitos (ex.: "123") era aceita
     * como documento válido, tanto no cadastro quanto na edição. Validamos
     * aqui a quantidade de dígitos exigida por cada documento (CPF = 11,
     * CNS = 15), sem checar o dígito verificador do CPF para não quebrar
     * dados de demonstração/importação já existentes.
     */
    /**
     * CPF/CNS são únicos no banco. Sem esta checagem, o cadastro repetido estourava a
     * constraint e o operador via só "registro com esse valor único já existe, tente
     * novamente". A mensagem não revela o nome do paciente existente: ele pode ser de
     * outra área de ACS.
     */
    private void exigirDocumentoDisponivel(String cpf, String cns, Long idAtual) {
        if (cpf != null && pacienteRepository.findByCpf(cpf).filter(p -> !p.getId().equals(idAtual)).isPresent()) {
            throw new IllegalStateException("Já existe um paciente cadastrado com este CPF. "
                    + "Procure-o na lista de pacientes ou peça ao regulador para vinculá-lo a você.");
        }
        if (cns != null && pacienteRepository.findByCns(cns).filter(p -> !p.getId().equals(idAtual)).isPresent()) {
            throw new IllegalStateException("Já existe um paciente cadastrado com este CNS. "
                    + "Procure-o na lista de pacientes ou peça ao regulador para vinculá-lo a você.");
        }
    }

    private void validarFormatoDocumento(String cpf, String cns, String cpfAtual, String cnsAtual) {
        if (cpf != null && cpf.length() != 11) {
            throw new IllegalStateException("CPF inválido: deve conter 11 dígitos.");
        }
        if (cns != null && cns.length() != 15) {
            throw new IllegalStateException("CNS inválido: deve conter 15 dígitos.");
        }
        if (cpf != null && !cpf.equals(cpfAtual) && !DocumentoValidador.cpfValido(cpf)) {
            throw new IllegalStateException("CPF inválido: os dígitos verificadores não conferem.");
        }
        if (cns != null && !cns.equals(cnsAtual) && !DocumentoValidador.cnsValido(cns)) {
            throw new IllegalStateException("CNS inválido: o número não confere com o padrão do Cartão Nacional de Saúde.");
        }
    }

    private Optional<Usuario> usuarioLogado() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .flatMap(usuarioRepository::findByEmailIgnoreCase);
    }
}
