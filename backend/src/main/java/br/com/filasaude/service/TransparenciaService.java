package br.com.filasaude.service;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.SlaConfiguracao;
import br.com.filasaude.domain.TransparenciaConfig;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.dto.dashboard.PrevisaoSlaResponse;
import br.com.filasaude.dto.transparencia.TransparenciaConfigRequest;
import br.com.filasaude.dto.transparencia.TransparenciaConfigResponse;
import br.com.filasaude.dto.transparencia.TransparenciaResponse;
import br.com.filasaude.dto.transparencia.TransparenciaResponse.AtendimentoMes;
import br.com.filasaude.dto.transparencia.TransparenciaResponse.Contagem;
import br.com.filasaude.dto.transparencia.TransparenciaResponse.CriterioFila;
import br.com.filasaude.dto.transparencia.TransparenciaResponse.Fila;
import br.com.filasaude.dto.transparencia.TransparenciaResponse.FilaProcedimento;
import br.com.filasaude.dto.transparencia.TransparenciaResponse.VagasUnidade;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.HorarioAgendaRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.repository.TransparenciaConfigRepository;
import br.com.filasaude.repository.UsuarioRepository;
import br.com.filasaude.tenancy.TenantContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Roadmap #6 -- Transparência ativa embutida (LAI). Publica indicadores
 * AGREGADOS da fila, sem qualquer dado de paciente. Reaproveita o motor de
 * priorização, o SLA configurável e a previsão do dashboard (#5), então os
 * números são os mesmos que o regulador enxerga.
 */
@Service
public class TransparenciaService {

    /** Grupos de 1 a LIMITE-1 pessoas saem como "menos de LIMITE" (LGPD). */
    static final int LIMITE_SUPRESSAO = 5;
    static final int MESES_HISTORICO = 12;
    static final int DIAS_VAGAS = 30;
    private static final long CACHE_MILIS = 60_000L;

    private record EntradaCache(long expiraEm, TransparenciaResponse resposta) {
    }

    private final Map<String, EntradaCache> cache = new ConcurrentHashMap<>();

    private final TransparenciaConfigRepository configRepository;
    private final FilaPriorizacaoService filaPriorizacaoService;
    private final SlaConfiguracaoService slaConfiguracaoService;
    private final PrevisaoSlaService previsaoSlaService;
    private final ProtocoloRepository protocoloRepository;
    private final HorarioAgendaRepository horarioAgendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public TransparenciaService(TransparenciaConfigRepository configRepository,
                                FilaPriorizacaoService filaPriorizacaoService,
                                SlaConfiguracaoService slaConfiguracaoService,
                                PrevisaoSlaService previsaoSlaService,
                                ProtocoloRepository protocoloRepository,
                                HorarioAgendaRepository horarioAgendaRepository,
                                UsuarioRepository usuarioRepository,
                                AuditoriaService auditoriaService) {
        this.configRepository = configRepository;
        this.filaPriorizacaoService = filaPriorizacaoService;
        this.slaConfiguracaoService = slaConfiguracaoService;
        this.previsaoSlaService = previsaoSlaService;
        this.protocoloRepository = protocoloRepository;
        this.horarioAgendaRepository = horarioAgendaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public TransparenciaConfigResponse obterConfig() {
        return TransparenciaConfigResponse.de(configAtual());
    }

    @Transactional
    public TransparenciaConfigResponse atualizarConfig(TransparenciaConfigRequest request) {
        TransparenciaConfig config = configRepository.findById(TransparenciaConfig.ID_UNICO)
                .orElseGet(() -> TransparenciaConfig.builder().build());
        boolean estavaPublicado = config.isPublicado();
        config.setPublicado(request.publicado());
        config.setMostrarFila(request.mostrarFila());
        config.setMostrarAtendimentos(request.mostrarAtendimentos());
        config.setMostrarVagas(request.mostrarVagas());
        config.setMostrarCriterios(request.mostrarCriterios());
        config.setTextoIntroducao(limpar(request.textoIntroducao()));
        config.setContatoLai(limpar(request.contatoLai()));
        config.setAtualizadoEm(LocalDateTime.now());
        usuarioLogado().ifPresent(config::setAtualizadoPor);
        configRepository.save(config);
        cache.remove(chaveTenant());

        String acao = request.publicado() == estavaPublicado
                ? "ATUALIZAR_TRANSPARENCIA"
                : (request.publicado() ? "PUBLICAR_TRANSPARENCIA" : "DESPUBLICAR_TRANSPARENCIA");
        auditoriaService.registrar(acao, "TransparenciaConfig", TransparenciaConfig.ID_UNICO,
                "Página de transparência " + (request.publicado() ? "publicada" : "não publicada")
                        + " (fila=" + request.mostrarFila() + ", atendimentos=" + request.mostrarAtendimentos()
                        + ", vagas=" + request.mostrarVagas() + ", critérios=" + request.mostrarCriterios() + ")");
        return TransparenciaConfigResponse.de(config);
    }

    /** Dados da página pública: 404 enquanto o secretário não publicar. Cache curto para não pesar o banco. */
    @Transactional(readOnly = true)
    public TransparenciaResponse gerarPublico() {
        String chave = chaveTenant();
        EntradaCache em = cache.get(chave);
        long agora = System.currentTimeMillis();
        if (em != null && em.expiraEm() > agora) {
            return em.resposta();
        }
        TransparenciaConfig config = configAtual();
        if (!config.isPublicado()) {
            throw new ResourceNotFoundException("A página de transparência ainda não foi publicada por esta prefeitura.");
        }
        TransparenciaResponse resposta = montar(config, LocalDate.now());
        cache.put(chave, new EntradaCache(agora + CACHE_MILIS, resposta));
        return resposta;
    }

    /** Prévia para o Admin: o que seria publicado, mesmo com a página ainda desligada. */
    @Transactional(readOnly = true)
    public TransparenciaResponse gerarPrevia() {
        return montar(configAtual(), LocalDate.now());
    }

    TransparenciaResponse montar(TransparenciaConfig config, LocalDate hoje) {
        return new TransparenciaResponse(
                LocalDateTime.now(),
                config.getTextoIntroducao(),
                config.getContatoLai(),
                LIMITE_SUPRESSAO,
                config.isMostrarFila() ? montarFila() : null,
                config.isMostrarAtendimentos() ? montarAtendimentos(hoje) : null,
                config.isMostrarVagas() ? montarVagas(hoje) : null,
                config.isMostrarCriterios() ? criterios() : null);
    }

    private Fila montarFila() {
        List<SlaConfiguracao> configuracoes = slaConfiguracaoService.carregarConfiguracoes();
        List<Protocolo> aguardando = filaPriorizacaoService.filaOrdenada();

        Map<Long, Integer> diasParaEsvaziar = previsaoSlaService.gerar().porProcedimento().stream()
                .filter(l -> l.diasParaEsvaziarFila() != null)
                .collect(Collectors.toMap(PrevisaoSlaResponse.PrevisaoProcedimento::procedimentoId,
                        PrevisaoSlaResponse.PrevisaoProcedimento::diasParaEsvaziarFila, (a, b) -> a));

        long dentro = 0, atencao = 0, fora = 0;
        Map<Long, List<Protocolo>> porProcedimento = new LinkedHashMap<>();
        for (Protocolo p : aguardando) {
            if (slaConfiguracaoService.estaAtrasado(p, configuracoes)) {
                fora++;
            } else if (slaConfiguracaoService.estaEmAtencao(p, configuracoes)) {
                atencao++;
            } else {
                dentro++;
            }
            porProcedimento.computeIfAbsent(p.getProcedimento().getId(), k -> new ArrayList<>()).add(p);
        }

        long total = aguardando.size();
        boolean publicavel = total >= LIMITE_SUPRESSAO;
        List<FilaProcedimento> linhas = new ArrayList<>();
        for (Map.Entry<Long, List<Protocolo>> e : porProcedimento.entrySet()) {
            List<Protocolo> grupo = e.getValue();
            boolean grupoPublicavel = grupo.size() >= LIMITE_SUPRESSAO;
            Protocolo ref = grupo.get(0);
            Integer media = null, maxima = null, esvaziar = null;
            if (grupoPublicavel) {
                media = (int) Math.round(grupo.stream().mapToLong(Protocolo::diasEmEspera).average().orElse(0));
                maxima = (int) grupo.stream().mapToLong(Protocolo::diasEmEspera).max().orElse(0);
                esvaziar = diasParaEsvaziar.get(e.getKey());
            }
            linhas.add(new FilaProcedimento(ref.getProcedimento().getNome(), ref.getProcedimento().getEspecialidade(),
                    Contagem.de(grupo.size(), LIMITE_SUPRESSAO), media, maxima, esvaziar));
        }
        linhas.sort((a, b) -> a.procedimento().compareToIgnoreCase(b.procedimento()));

        return new Fila(Contagem.de(total, LIMITE_SUPRESSAO),
                publicavel ? percentual(dentro, total) : null,
                publicavel ? percentual(atencao, total) : null,
                publicavel ? percentual(fora, total) : null,
                linhas);
    }

    private static int percentual(long parte, long total) {
        return total == 0 ? 0 : (int) Math.round(parte * 100.0 / total);
    }

    private List<AtendimentoMes> montarAtendimentos(LocalDate hoje) {
        YearMonth atual = YearMonth.from(hoje);
        YearMonth primeiro = atual.minusMonths(MESES_HISTORICO - 1L);
        Map<YearMonth, Long> porMes = new LinkedHashMap<>();
        for (Object[] linha : protocoloRepository.contarConcluidosPorMes(primeiro.atDay(1).atStartOfDay())) {
            porMes.put(YearMonth.of(((Number) linha[0]).intValue(), ((Number) linha[1]).intValue()),
                    ((Number) linha[2]).longValue());
        }
        List<AtendimentoMes> meses = new ArrayList<>();
        for (YearMonth m = primeiro; !m.isAfter(atual); m = m.plusMonths(1)) {
            meses.add(new AtendimentoMes(m.toString(), Contagem.de(porMes.getOrDefault(m, 0L), LIMITE_SUPRESSAO)));
        }
        return meses;
    }

    private List<VagasUnidade> montarVagas(LocalDate hoje) {
        LocalDate fim = hoje.plusDays(DIAS_VAGAS);
        String periodo = "próximos " + DIAS_VAGAS + " dias";
        List<VagasUnidade> vagas = new ArrayList<>();
        for (Object[] linha : horarioAgendaRepository.somarVagasPorUnidade(hoje, fim)) {
            vagas.add(new VagasUnidade((String) linha[0], ((Number) linha[1]).longValue(), periodo));
        }
        return vagas;
    }

    static List<CriterioFila> criterios() {
        return List.of(
                new CriterioFila(1, "Urgência", "Risco clínico avaliado. Quando pedida por agente comunitário, só vale após confirmação do regulador."),
                new CriterioFila(2, "Judicial", "Determinação judicial com número de processo. Quando pedida por agente comunitário, só vale após confirmação do regulador."),
                new CriterioFila(3, "Especial (80+)", "Pacientes com 80 anos ou mais."),
                new CriterioFila(4, "Legal", "Pacientes com 60 anos ou mais, pessoas com deficiência e gestantes."),
                new CriterioFila(5, "Normal", "Demais casos, por ordem cronológica de solicitação."));
    }

    /**
     * Exportação em CSV (separador ";", UTF-8 com BOM -- abre direto no Excel em português).
     * Formato longo: secao;indicador;detalhe;valor.
     */
    public String paraCsv(TransparenciaResponse t) {
        StringBuilder sb = new StringBuilder("﻿secao;indicador;detalhe;valor\n");
        if (t.fila() != null) {
            Fila f = t.fila();
            linha(sb, "Fila", "Aguardando atendimento", "total", f.totalAguardando().rotulo());
            if (f.percentualDentroDoPrazo() != null) {
                linha(sb, "Fila", "Dentro do prazo (%)", "total", String.valueOf(f.percentualDentroDoPrazo()));
                linha(sb, "Fila", "Em atenção (%)", "total", String.valueOf(f.percentualEmAtencao()));
                linha(sb, "Fila", "Fora do prazo (%)", "total", String.valueOf(f.percentualForaDoPrazo()));
            }
            for (FilaProcedimento p : f.porProcedimento()) {
                String det = p.procedimento() + (p.especialidade() != null ? " (" + p.especialidade() + ")" : "");
                linha(sb, "Fila", "Aguardando", det, p.aguardando().rotulo());
                if (p.esperaMediaDias() != null) {
                    linha(sb, "Fila", "Espera média (dias)", det, String.valueOf(p.esperaMediaDias()));
                    linha(sb, "Fila", "Espera máxima (dias)", det, String.valueOf(p.esperaMaximaDias()));
                }
                if (p.diasParaEsvaziarFila() != null) {
                    linha(sb, "Fila", "Dias para esvaziar a fila (estimativa)", det, String.valueOf(p.diasParaEsvaziarFila()));
                }
            }
        }
        if (t.atendimentos() != null) {
            for (AtendimentoMes a : t.atendimentos()) {
                linha(sb, "Atendimentos", "Concluídos no mês", a.mes(), a.concluidos().rotulo());
            }
        }
        if (t.vagas() != null) {
            for (VagasUnidade v : t.vagas()) {
                linha(sb, "Vagas", "Vagas ofertadas (" + v.periodo() + ")", v.unidade(), String.valueOf(v.vagasOfertadas()));
            }
        }
        if (t.criterios() != null) {
            for (CriterioFila c : t.criterios()) {
                linha(sb, "Critérios da fila", c.ordem() + "ª prioridade", c.categoria(), c.descricao());
            }
        }
        return sb.toString();
    }

    private static void linha(StringBuilder sb, String... celulas) {
        for (int i = 0; i < celulas.length; i++) {
            if (i > 0) sb.append(';');
            sb.append(celulaCsv(celulas[i]));
        }
        sb.append('\n');
    }

    /** Escapa a célula e neutraliza injeção de fórmula (=, +, -, @) em planilhas. */
    static String celulaCsv(String valor) {
        String v = valor == null ? "" : valor.replace("\r", " ").replace("\n", " ");
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(";") || v.contains("\"")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private TransparenciaConfig configAtual() {
        return configRepository.findById(TransparenciaConfig.ID_UNICO)
                .orElseGet(() -> TransparenciaConfig.builder().build());
    }

    private static String limpar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private static String chaveTenant() {
        String t = TenantContext.getCurrentTenant();
        return t == null ? "_" : t;
    }

    private java.util.Optional<Usuario> usuarioLogado() {
        return java.util.Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(a -> a.getName())
                .flatMap(usuarioRepository::findByEmailIgnoreCase);
    }
}
