package br.com.filasaude.service;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.SlaConfiguracao;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.dashboard.PrevisaoSlaResponse;
import br.com.filasaude.repository.HistoricoStatusRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Roadmap #5 -- Dashboard preditivo de SLA. Ver o Javadoc de
 * {@link PrevisaoSlaResponse} para o método. Reaproveita o motor de
 * priorização (ordem da fila) e o {@link SlaConfiguracaoService} (limite de
 * SLA por protocolo), para nunca divergir do que a Fila e o Dashboard
 * chamam de "Atrasado".
 */
@Service
@Transactional(readOnly = true)
public class PrevisaoSlaService {

    /** Janela de histórico usada para medir a vazão da fila. */
    static final int JANELA_DIAS = 90;
    static final int MAX_PROTOCOLOS_EM_RISCO = 10;

    private enum Risco { JA_ATRASADO, VAI_ATRASAR, NO_PRAZO, SEM_HISTORICO }

    private final ProtocoloRepository protocoloRepository;
    private final HistoricoStatusRepository historicoStatusRepository;
    private final FilaPriorizacaoService filaPriorizacaoService;
    private final SlaConfiguracaoService slaConfiguracaoService;

    public PrevisaoSlaService(ProtocoloRepository protocoloRepository,
                               HistoricoStatusRepository historicoStatusRepository,
                               FilaPriorizacaoService filaPriorizacaoService,
                               SlaConfiguracaoService slaConfiguracaoService) {
        this.protocoloRepository = protocoloRepository;
        this.historicoStatusRepository = historicoStatusRepository;
        this.filaPriorizacaoService = filaPriorizacaoService;
        this.slaConfiguracaoService = slaConfiguracaoService;
    }

    public PrevisaoSlaResponse gerar() {
        return gerar(LocalDate.now());
    }

    PrevisaoSlaResponse gerar(LocalDate hoje) {
        LocalDate desdeData = hoje.minusDays(JANELA_DIAS);
        LocalDateTime desde = desdeData.atStartOfDay();

        Map<Long, Long> saidas = paraMapa(historicoStatusRepository.contarSaidasDaFila(
                StatusProtocolo.AGUARDANDO,
                List.of(StatusProtocolo.AGENDADO, StatusProtocolo.EM_ANDAMENTO, StatusProtocolo.CONCLUIDO),
                desde));
        Map<Long, Long> entradas = paraMapa(protocoloRepository.contarEntradasDesde(desdeData));

        List<SlaConfiguracao> configuracoes = slaConfiguracaoService.carregarConfiguracoes();

        // Fila já ordenada pelo motor de priorização, agrupada por procedimento
        // (a posição é por procedimento -- ver FilaPriorizacaoService).
        Map<Long, List<Protocolo>> filasPorProcedimento = filaPriorizacaoService.filaOrdenada().stream()
                .collect(Collectors.groupingBy(p -> p.getProcedimento().getId(), LinkedHashMap::new, Collectors.toList()));

        List<PrevisaoSlaResponse.PrevisaoProcedimento> linhas = new ArrayList<>();
        List<PrevisaoSlaResponse.ProtocoloEmRisco> emRisco = new ArrayList<>();
        long totalAguardando = 0, totalAtrasados = 0, totalVaoAtrasar = 0, totalSemHistorico = 0, totalNoPrazo = 0;

        for (List<Protocolo> fila : filasPorProcedimento.values()) {
            Protocolo primeiro = fila.get(0);
            Long procedimentoId = primeiro.getProcedimento().getId();
            long saidasNaJanela = saidas.getOrDefault(procedimentoId, 0L);
            long entradasNaJanela = entradas.getOrDefault(procedimentoId, 0L);
            double vazaoDia = saidasNaJanela / (double) JANELA_DIAS;

            long jaAtrasados = 0, vaoAtrasar = 0, semHistorico = 0, noPrazo = 0;
            for (int i = 0; i < fila.size(); i++) {
                Protocolo p = fila.get(i);
                int posicao = i + 1;
                long dias = Math.max(0, ChronoUnit.DAYS.between(p.getDataInclusao(), hoje));
                int limite = slaConfiguracaoService.resolverLimites(p, configuracoes).diasAtrasado();

                Risco risco;
                int atendimentoEm = 0;
                if (dias > limite) {
                    risco = Risco.JA_ATRASADO;
                } else if (saidasNaJanela == 0) {
                    risco = Risco.SEM_HISTORICO;
                } else {
                    atendimentoEm = (int) Math.ceil(posicao / vazaoDia);
                    risco = dias + atendimentoEm > limite ? Risco.VAI_ATRASAR : Risco.NO_PRAZO;
                }

                switch (risco) {
                    case JA_ATRASADO -> jaAtrasados++;
                    case VAI_ATRASAR -> {
                        vaoAtrasar++;
                        emRisco.add(new PrevisaoSlaResponse.ProtocoloEmRisco(
                                p.getId(), p.getNumeroProtocolo(), p.getPaciente().getNome(),
                                p.getProcedimento().getNome(), p.getCategoriaPrioridade().name(),
                                posicao, dias, limite, limite - dias + 1, atendimentoEm));
                    }
                    case SEM_HISTORICO -> semHistorico++;
                    case NO_PRAZO -> noPrazo++;
                }
            }

            totalAguardando += fila.size();
            totalAtrasados += jaAtrasados;
            totalVaoAtrasar += vaoAtrasar;
            totalSemHistorico += semHistorico;
            totalNoPrazo += noPrazo;

            Integer diasParaEsvaziar = saidasNaJanela > 0 ? (int) Math.ceil(fila.size() / vazaoDia) : null;
            linhas.add(new PrevisaoSlaResponse.PrevisaoProcedimento(
                    procedimentoId, primeiro.getProcedimento().getNome(), primeiro.getProcedimento().getEspecialidade(),
                    fila.size(), entradasNaJanela, saidasNaJanela,
                    Math.round(vazaoDia * 7 * 10.0) / 10.0, diasParaEsvaziar, jaAtrasados, vaoAtrasar,
                    tendencia(entradasNaJanela, saidasNaJanela)));
        }

        linhas.sort(Comparator
                .comparingLong((PrevisaoSlaResponse.PrevisaoProcedimento l) -> l.jaAtrasados() + l.vaoAtrasar()).reversed()
                .thenComparing(PrevisaoSlaResponse.PrevisaoProcedimento::procedimento));
        emRisco.sort(Comparator
                .comparingLong(PrevisaoSlaResponse.ProtocoloEmRisco::diasAteAtraso)
                .thenComparingInt(PrevisaoSlaResponse.ProtocoloEmRisco::posicaoFila));

        long comBase = totalNoPrazo + totalVaoAtrasar + totalAtrasados;
        Integer percentual = comBase > 0 ? (int) Math.round(totalNoPrazo * 100.0 / comBase) : null;

        return new PrevisaoSlaResponse(JANELA_DIAS,
                new PrevisaoSlaResponse.Resumo(totalAguardando, totalAtrasados, totalVaoAtrasar, totalSemHistorico, percentual),
                linhas,
                emRisco.stream().limit(MAX_PROTOCOLOS_EM_RISCO).toList());
    }

    private static String tendencia(long entradas, long saidas) {
        if (entradas == 0 && saidas == 0) {
            return "SEM_DADOS";
        }
        if (entradas > saidas) {
            return "CRESCENDO";
        }
        return entradas == saidas ? "ESTAVEL" : "DIMINUINDO";
    }

    private static Map<Long, Long> paraMapa(List<Object[]> linhas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] l : linhas) {
            mapa.put(((Number) l[0]).longValue(), ((Number) l[1]).longValue());
        }
        return mapa;
    }
}
