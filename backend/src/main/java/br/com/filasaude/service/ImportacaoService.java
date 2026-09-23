package br.com.filasaude.service;

import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.enums.TipoProcedimento;
import br.com.filasaude.dto.importacao.ImportacaoErro;
import br.com.filasaude.dto.importacao.ImportacaoResponse;
import br.com.filasaude.repository.PacienteRepository;
import br.com.filasaude.repository.ProcedimentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final AuditoriaService auditoriaService;

    public ImportacaoService(ProcedimentoRepository procedimentoRepository, PacienteRepository pacienteRepository,
                              AuditoriaService auditoriaService) {
        this.procedimentoRepository = procedimentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.auditoriaService = auditoriaService;
    }

    public ImportacaoResponse importarProcedimentos(MultipartFile arquivo) {
        List<Map<String, String>> linhas = lerLinhas(arquivo);
        List<ImportacaoErro> erros = new ArrayList<>();
        int importados = 0;
        int numeroLinha = 1;

        for (Map<String, String> linha : linhas) {
            numeroLinha++;
            String nome = linha.getOrDefault("nome", "");
            String tipoTexto = linha.getOrDefault("tipo", "");
            String especialidade = linha.get("especialidade");

            if (nome.isBlank()) {
                erros.add(new ImportacaoErro(numeroLinha, "Coluna 'nome' é obrigatória"));
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
        List<Map<String, String>> linhas = lerLinhas(arquivo);
        List<ImportacaoErro> erros = new ArrayList<>();
        int importados = 0;
        int numeroLinha = 1;

        for (Map<String, String> linha : linhas) {
            numeroLinha++;
            String nome = linha.getOrDefault("nome", "");
            String cpf = somenteDigitos(linha.get("cpf"));
            String cns = somenteDigitos(linha.get("cns"));
            String telefone = vazioParaNulo(linha.get("telefone"));
            String email = vazioParaNulo(linha.get("email"));
            String dataTexto = linha.get("datanascimento");

            if (nome.isBlank()) {
                erros.add(new ImportacaoErro(numeroLinha, "Coluna 'nome' é obrigatória"));
                continue;
            }
            if (cpf == null && cns == null) {
                erros.add(new ImportacaoErro(numeroLinha, "Informe ao menos CPF ou CNS"));
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

    private List<Map<String, String>> lerLinhas(MultipartFile arquivo) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {

            String cabecalhoLinha = reader.readLine();
            if (cabecalhoLinha == null || cabecalhoLinha.isBlank()) {
                return List.of();
            }
            if (cabecalhoLinha.startsWith("﻿")) {
                cabecalhoLinha = cabecalhoLinha.substring(1);
            }

            char delimitador = detectarDelimitador(cabecalhoLinha);
            String[] cabecalhos = dividirLinha(cabecalhoLinha, delimitador);

            List<Map<String, String>> linhas = new ArrayList<>();
            String linhaTexto;
            while ((linhaTexto = reader.readLine()) != null) {
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
                linhas.add(mapa);
            }
            return linhas;
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o arquivo enviado", e);
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
