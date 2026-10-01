package br.com.filasaude.service;

import br.com.filasaude.domain.Paciente;
import br.com.filasaude.dto.paciente.PacienteDuplicadoResponse;
import br.com.filasaude.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Detecção de duplicidade fuzzy de paciente (item do roadmap). A checagem
 * de duplicidade já existente no sistema (ver ProcedimentoController) só
 * cobre nome de PROCEDIMENTO, exato e case-insensitive -- adequada porque
 * nome de procedimento é um vocabulário controlado e pequeno. Nome de
 * PACIENTE não é: dois cadastros digitados em momentos diferentes (ex.: um
 * ACS e depois outro, ou o mesmo ACS duas vezes) raramente ficam
 * caractere-por-caractere iguais, mesmo sendo a mesma pessoa -- "Maria
 * Silva" vs "Maria da Silva", um acento faltando, uma letra trocada. Por
 * isso aqui a comparação de nome é por SEMELHANÇA (distância de Levenshtein
 * normalizada), não por igualdade, corroborada por data de nascimento ou
 * telefone quando disponíveis.
 *
 * Deliberadamente não bloqueia nada: é consultado pelo frontend ANTES de
 * enviar o cadastro, para avisar o operador e deixar a decisão final com
 * ele (usar o paciente já existente, ou confirmar que são pessoas
 * diferentes e cadastrar mesmo assim) -- diferente de CPF/CNS duplicado,
 * que é uma constraint exata e por isso pode seguir bloqueando no banco.
 */
@Service
@Transactional(readOnly = true)
public class DuplicidadePacienteService {

    /**
     * Nome semelhante sozinho, sem nenhuma outra coincidência, precisa ser
     * MUITO parecido para virar alerta (evita falso positivo entre
     * "João Silva" e "João Souza", que são pessoas comuns e diferentes).
     */
    private static final double LIMIAR_NOME_ISOLADO = 0.92;

    /**
     * Quando a data de nascimento OU o telefone também coincidem, um nome
     * só razoavelmente parecido já é um indício forte de duplicidade.
     */
    private static final double LIMIAR_NOME_CORROBORADO = 0.78;

    private static final int MAXIMO_RESULTADOS = 5;

    private final PacienteRepository pacienteRepository;

    public DuplicidadePacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    /**
     * @param ignorarPacienteId exclui esse paciente da comparação -- usado ao
     *                          EDITAR um cadastro, para ele não aparecer como
     *                          "duplicado" de si mesmo.
     */
    public List<PacienteDuplicadoResponse> buscarPossiveisDuplicados(
            String nome, LocalDate dataNascimento, String telefone, Long ignorarPacienteId) {
        if (nome == null || nome.isBlank()) {
            return List.of();
        }
        String nomeNormalizado = normalizar(nome);
        String telefoneDigitos = somenteDigitosOuNulo(telefone);

        List<PacienteDuplicadoResponse> encontrados = new ArrayList<>();
        for (Paciente candidato : pacienteRepository.findAll()) {
            if (ignorarPacienteId != null && ignorarPacienteId.equals(candidato.getId())) {
                continue;
            }
            PacienteDuplicadoResponse avaliado = avaliar(candidato, nomeNormalizado, dataNascimento, telefoneDigitos);
            if (avaliado != null) {
                encontrados.add(avaliado);
            }
        }

        return encontrados.stream()
                .sorted(Comparator.comparingInt(PacienteDuplicadoResponse::pontuacaoSimilaridade).reversed())
                .limit(MAXIMO_RESULTADOS)
                .toList();
    }

    private PacienteDuplicadoResponse avaliar(Paciente candidato, String nomeNormalizadoNovo,
                                               LocalDate dataNascimentoNovo, String telefoneDigitosNovo) {
        double similaridadeNome = similaridade(nomeNormalizadoNovo, normalizar(candidato.getNome()));

        boolean mesmaDataNascimento = dataNascimentoNovo != null
                && dataNascimentoNovo.equals(candidato.getDataNascimento());
        boolean mesmoTelefone = telefoneDigitosNovo != null
                && telefoneDigitosNovo.equals(somenteDigitosOuNulo(candidato.getTelefone()));
        boolean corroborado = mesmaDataNascimento || mesmoTelefone;

        double limiar = corroborado ? LIMIAR_NOME_CORROBORADO : LIMIAR_NOME_ISOLADO;
        if (similaridadeNome < limiar) {
            return null;
        }

        List<String> motivos = new ArrayList<>();
        motivos.add("nome semelhante");
        if (mesmaDataNascimento) {
            motivos.add("mesma data de nascimento");
        }
        if (mesmoTelefone) {
            motivos.add("mesmo telefone");
        }

        return new PacienteDuplicadoResponse(
                candidato.getId(), candidato.getNome(), candidato.getCpf(), candidato.getCns(),
                candidato.getDataNascimento(), candidato.getTelefone(),
                (int) Math.round(similaridadeNome * 100), String.join(" + ", motivos));
    }

    private double similaridade(String a, String b) {
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 1.0;
        }
        return 1.0 - ((double) levenshtein(a, b) / maxLen);
    }

    /** Distância de Levenshtein clássica, em O(n*m) tempo e O(m) memória (duas linhas rotativas). */
    private int levenshtein(String a, String b) {
        int[] anterior = new int[b.length() + 1];
        int[] atual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            anterior[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            atual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int custoSubstituicao = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                atual[j] = Math.min(Math.min(anterior[j] + 1, atual[j - 1] + 1), anterior[j - 1] + custoSubstituicao);
            }
            int[] troca = anterior;
            anterior = atual;
            atual = troca;
        }
        return anterior[b.length()];
    }

    /** Minúsculas, sem acento, espaços nas pontas e internos normalizados -- "José  DA Silva " == "jose da silva". */
    private String normalizar(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String somenteDigitosOuNulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.isBlank() ? null : digitos;
    }
}
