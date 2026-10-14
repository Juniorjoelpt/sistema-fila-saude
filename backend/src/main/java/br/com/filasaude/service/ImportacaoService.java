package br.com.filasaude.service;

import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.enums.TipoProcedimento;
import br.com.filasaude.dto.importacao.ImportacaoErro;
import br.com.filasaude.dto.importacao.ImportacaoResponse;
import br.com.filasaude.repository.EspecialidadeRepository;
import br.com.filasaude.repository.PacienteRepository;
import br.com.filasaude.repository.ProcedimentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Importação em lote de procedimentos e pacientes via planilha CSV (item
 * 3.4 do levantamento de requisitos, Fase 2). Aceita ';' ou ',' como
 * separador (detectado automaticamente pelo cabeçalho) e colunas em
 * qualquer ordem, identificadas pelo nome.
 *
 * Cada linha é processada e salva de forma independente (sem uma
 * transação única para o arquivo inteiro): se uma linha falhar (dado
 * inválido, duplicado etc.), ela entra no relatório de erros e as demais
 * continuam sendo importadas normalmente.
 */
@Service
public class ImportacaoService {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoService.class);
    private static final List<DateTimeFormatter> FORMATOS_DATA = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd
    );

    private final ProcedimentoRepository procedimentoRepository;
    private final PacienteRepository pacienteRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final AuditoriaService auditoriaService;

    public ImportacaoService(ProcedimentoRepository procedimentoRepository, PacienteRepository pacienteRepository,
                              EspecialidadeRepository especialidadeRepository, AuditoriaService auditoriaService) {
        this.procedimentoRepository = procedimentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.especialidadeRepository = especialidadeRepository;
        this.auditoriaService = auditoriaService;
    }

    public ImportacaoResponse importarProcedimentos(MultipartFile arquivo) {
        List<LinhaCsv> linhas = lerLinhas(arquivo);
        List<ImportacaoErro> erros = new ArrayList<>();
        int importados = 0;
        // Bug alto de revisão corrigido: duas linhas com o mesmo nome de
        // procedimento DENTRO do mesmo arquivo passavam as duas -- a
        // constraint do banco (quando existe) só pega a segunda tentativa de
        // save, mas antes disso não havia nenhum aviso; agora ambas as
        // linhas duplicadas viram erro, mantendo o comportamento simétrico
        // ao de ProcedimentoController#criar (que também rejeita duplicado).
        Set<String> nomesNesteArquivo = new HashSet<>();

        for (LinhaCsv linha : linhas) {
            int numeroLinha = linha.numero();
            if (linha.colunasExtras()) {
                erros.add(new ImportacaoErro(numeroLinha,
                        "Linha tem mais colunas que o cabeçalho -- dados extras foram descartados; verifique se há vírgula/ponto-e-vírgula sem aspas em algum campo"));
            }
            String nome = linha.valores().getOrDefault("nome", "");
            String tipoTexto = linha.valores().getOrDefault("tipo", "");
            String especialidade = linha.valores().get("especialidade");

            if (nome.isBlank()) {
                erros.add(new ImportacaoErro(numeroLinha, "Coluna 'nome' é obrigatória"));
                continue;
            }

            String chaveNome = nome.trim().toLowerCase();
            if (!nomesNesteArquivo.add(chaveNome)) {
                erros.add(new ImportacaoErro(numeroLinha, "Nome de procedimento duplicado neste arquivo: " + nome.trim()));
                continue;
            }
            if (procedimentoRepository.findByNomeIgnoreCase(nome.trim()).isPresent()) {
                erros.add(new ImportacaoErro(numeroLinha, "Já existe um procedimento cadastrado com este nome: " + nome.trim()));
                continue;
            }

            TipoProcedimento tipo;
            try {
                tipo = TipoProcedimento.valueOf(tipoTexto.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                erros.add(new ImportacaoErro(numeroLinha,
                        "Coluna 'tipo' inválida (use CONSULTA, EXAME ou CIRURGIA)"));
                continue;
            }

            // Gap de revisão corrigido: mesma validação do cadastro manual
            // (ver ProcedimentoController) -- sem ela, a importação em lote
            // era a porta mais fácil para uma especialidade digitada errado
            // entrar sem nenhum aviso, já que ninguém revisa linha a linha
            // de um CSV com centenas de procedimentos.
            String especialidadeNormalizada = vazioParaNulo(especialidade);
            if (especialidadeNormalizada != null
                    && especialidadeRepository.findByNomeIgnoreCase(especialidadeNormalizada.trim())
                            .filter(e -> e.isAtivo()).isEmpty()) {
                erros.add(new ImportacaoErro(numeroLinha,
                        "Especialidade não cadastrada ou inativa: " + especialidadeNormalizada.trim()
                                + " -- cadastre-a antes em \"Cadastros > Especialidades\""));
                continue;
            }

            try {
                procedimentoRepository.save(Procedimento.builder()
                        .nome(nome.trim())
                        .tipo(tipo)
                        .especialidade(vazioParaNulo(especialidade))
                        .ativo(true)
                        .build());
                importados++;
            } catch (Exception e) {
                log.warn("Falha ao importar linha {} de procedimentos: {}", numeroLinha, e.getMessage());
                erros.add(new ImportacaoErro(numeroLinha, "Não foi possível salvar esta linha"));
            }
        }

        auditoriaService.registrar("IMPORTACAO_LOTE", "Procedimento", null,
                importados + " de " + linhas.size() + " procedimento(s) importado(s) via CSV");
        return new ImportacaoResponse(linhas.size(), importados, erros);
    }

    public ImportacaoResponse importarPacientes(MultipartFile arquivo) {
        List<LinhaCsv> linhas = lerLinhas(arquivo);
        List<ImportacaoErro> erros = new ArrayList<>();
        int importados = 0;

        for (LinhaCsv linha : linhas) {
            int numeroLinha = linha.numero();
            if (linha.colunasExtras()) {
                erros.add(new ImportacaoErro(numeroLinha,
                        "Linha tem mais colunas que o cabeçalho -- dados extras foram descartados; verifique se há vírgula/ponto-e-vírgula sem aspas em algum campo"));
            }
            String nome = linha.valores().getOrDefault("nome", "");
            String cpf = somenteDigitos(linha.valores().get("cpf"));
            String cns = somenteDigitos(linha.valores().get("cns"));
            String telefone = vazioParaNulo(linha.valores().get("telefone"));
            String email = vazioParaNulo(linha.valores().get("email"));
            String dataTexto = linha.valores().get("datanascimento");

            if (nome.isBlank()) {
                erros.add(new ImportacaoErro(numeroLinha, "Coluna 'nome' é obrigatória"));
                continue;
            }
            if (cpf == null && cns == null) {
                erros.add(new ImportacaoErro(numeroLinha, "Informe ao menos CPF ou CNS"));
                continue;
            }
            if (cpf != null && !DocumentoValidador.cpfValido(cpf)) {
                erros.add(new ImportacaoErro(numeroLinha, "CPF inválido (11 dígitos com dígitos verificadores corretos)"));
                continue;
            }
            if (cns != null && !DocumentoValidador.cnsValido(cns)) {
                erros.add(new ImportacaoErro(numeroLinha, "CNS inválido (15 dígitos no padrão do Cartão Nacional de Saúde)"));
                continue;
            }

            LocalDate dataNascimento = null;
            if (dataTexto != null && !dataTexto.isBlank()) {
                dataNascimento = parseData(dataTexto.trim());
                if (dataNascimento == null) {
                    erros.add(new ImportacaoErro(numeroLinha,
                            "Data de nascimento inválida (use dd/mm/aaaa ou aaaa-mm-dd)"));
                    continue;
                }
            }

            try {
                pacienteRepository.save(Paciente.builder()
                        .nome(nome.trim())
                        .cpf(cpf)
                        .cns(cns)
                        .dataNascimento(dataNascimento)
                        .telefone(telefone)
                        .email(email)
                        .build());
                importados++;
            } catch (Exception e) {
                log.warn("Falha ao importar linha {} de pacientes: {}", numeroLinha, e.getMessage());
                erros.add(new ImportacaoErro(numeroLinha,
                        "Não foi possível salvar esta linha (CPF/CNS já cadastrado?)"));
            }
        }

        auditoriaService.registrar("IMPORTACAO_LOTE", "Paciente", null,
                importados + " de " + linhas.size() + " paciente(s) importado(s) via CSV");
        return new ImportacaoResponse(linhas.size(), importados, erros);
    }

    private LocalDate parseData(String texto) {
        for (DateTimeFormatter formato : FORMATOS_DATA) {
            try {
                return LocalDate.parse(texto, formato);
            } catch (DateTimeParseException ignored) {
                // tenta o proximo formato
            }
        }
        return null;
    }

    private String somenteDigitos(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.isBlank() ? null : digitos;
    }

    private String vazioParaNulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    /** Uma linha de dados do CSV, com o número de linha REAL do arquivo (para o relatório de erros). */
    private record LinhaCsv(int numero, Map<String, String> valores, boolean colunasExtras) {
    }

    private List<LinhaCsv> lerLinhas(MultipartFile arquivo) {
        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o arquivo enviado", e);
        }

        Charset charset = detectarCodificacao(bytes);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(bytes), charset))) {

            String cabecalhoLinha = reader.readLine();
            if (cabecalhoLinha == null || cabecalhoLinha.isBlank()) {
                return List.of();
            }
            if (cabecalhoLinha.startsWith("﻿")) {
                cabecalhoLinha = cabecalhoLinha.substring(1);
            }

            char delimitador = detectarDelimitador(cabecalhoLinha);
            String[] cabecalhos = dividirLinha(cabecalhoLinha, delimitador);

            // Bug alto de revisão corrigido: a numeração de linha reportada nos
            // erros era baseada na posição dentro da lista JÁ FILTRADA (sem as
            // linhas em branco), então um arquivo com uma linha em branco no
            // meio reportava o número errado para todas as linhas seguintes,
            // levando o usuário a "corrigir" a linha errada na planilha.
            // Contando a partir do próprio cabeçalho (linha 1) e incrementando
            // a cada leitura -- em branco ou não -- mantém o número real.
            List<LinhaCsv> linhas = new ArrayList<>();
            String linhaTexto;
            int numeroLinhaArquivo = 1;
            while ((linhaTexto = reader.readLine()) != null) {
                numeroLinhaArquivo++;
                if (linhaTexto.isBlank()) {
                    continue;
                }
                String[] valores = dividirLinha(linhaTexto, delimitador);
                Map<String, String> mapa = new LinkedHashMap<>();
                for (int i = 0; i < cabecalhos.length; i++) {
                    String chave = normalizarCabecalho(cabecalhos[i]);
                    String valor = i < valores.length ? valores[i].trim() : "";
                    mapa.put(chave, valor);
                }
                // Bug médio de revisão corrigido: quando a linha tinha MAIS
                // colunas que o cabeçalho (ex.: vírgula sem aspas dentro de um
                // campo de texto livre), os valores extras eram simplesmente
                // ignorados pelo loop acima -- perda silenciosa de dado. Agora
                // fica marcado para virar um erro visível no relatório.
                boolean colunasExtras = valores.length > cabecalhos.length;
                linhas.add(new LinhaCsv(numeroLinhaArquivo, mapa, colunasExtras));
            }
            return linhas;
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o arquivo enviado", e);
        }
    }

    /**
     * Bug alto de revisão corrigido: o arquivo era sempre lido como UTF-8,
     * sem nenhuma detecção -- um CSV exportado do Excel brasileiro (comum:
     * Windows-1252/ISO-8859-1) com acentos virava texto corrompido
     * silenciosamente (nenhum erro, só "nome" errado no banco). Detecta BOM
     * de UTF-8/UTF-16 quando presente; sem BOM, tenta decodificar como UTF-8
     * estrito e, se encontrar uma sequência de bytes inválida (sinal de que
     * não é UTF-8), cai para Windows-1252 como alternativa mais comum em
     * planilhas brasileiras.
     */
    private Charset detectarCodificacao(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            return StandardCharsets.UTF_16BE;
        }

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(java.nio.ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            log.info("Arquivo CSV não é UTF-8 válido; usando Windows-1252 como alternativa");
            return Charset.forName("windows-1252");
        }
    }

    private char detectarDelimitador(String cabecalhoLinha) {
        long virgulas = cabecalhoLinha.chars().filter(c -> c == ',').count();
        long pontoEVirgulas = cabecalhoLinha.chars().filter(c -> c == ';').count();
        return pontoEVirgulas >= virgulas ? ';' : ',';
    }

    private String[] dividirLinha(String linha, char delimitador) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean dentroAspas = false;

        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '"') {
                if (dentroAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else {
                    dentroAspas = !dentroAspas;
                }
            } else if (c == delimitador && !dentroAspas) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString());
        return campos.toArray(new String[0]);
    }

    private String normalizarCabecalho(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim().toLowerCase()
                .replace("_", "")
                .replace(" ", "")
                .replace("ç", "c")
                .replace("ã", "a")
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o");
    }
}
