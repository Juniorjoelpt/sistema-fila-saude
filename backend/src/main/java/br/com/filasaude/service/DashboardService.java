package br.com.filasaude.service;

import br.com.filasaude.domain.Cota;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.PresencaConfirmacao;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.dashboard.DashboardResponse;
import br.com.filasaude.repository.CotaRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Indicadores do painel administrativo municipal (item 3.4). A taxa de
 * ocupação por especialidade usa as cotas cadastradas do mês corrente
 * (item 3.3, Fase 2) e destaca gargalos (especialidades perto ou acima de
 * 100% da cota).
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** Percentual a partir do qual uma especialidade é sinalizada como gargalo. */
    private static final int LIMIAR_GARGALO = 90;

    private final ProtocoloRepository protocoloRepository;
    private final CotaRepository cotaRepository;
    private final int diasLimiteAtrasado;

    public DashboardService(ProtocoloRepository protocoloRepository, CotaRepository cotaRepository,
                             @Value("${filasaude.sla.dias-limite-atrasado:15}") int diasLimiteAtrasado) {
        this.protocoloRepository = protocoloRepository;
        this.cotaRepository = cotaRepository;
        this.diasLimiteAtrasado = diasLimiteAtrasado;
    }

    public DashboardResponse gerar() {
        long filaDeEspera = protocoloRepository.countByStatus(StatusProtocolo.AGUARDANDO);

        LocalDate hoje = LocalDate.now();
        long agendadosNoMes = protocoloRepository.findByStatus(StatusProtocolo.AGENDADO).stream()
                .filter(p -> p.getDataPrevista() != null
                        && p.getDataPrevista().getMonth() == hoje.getMonth()
                        && p.getDataPrevista().getYear() == hoje.getYear())
                .count();

        long realizadosNoAno = protocoloRepository.findByStatus(StatusProtocolo.CONCLUIDO).stream()
                .filter(p -> p.getAtualizadoEm().getYear() == hoje.getYear())
                .count();

        List<Protocolo> aguardando = protocoloRepository.findByStatus(StatusProtocolo.AGUARDANDO);
        Map<String, Long> porEspecialidade = aguardando.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getProcedimento().getEspecialidade() != null
                                ? p.getProcedimento().getEspecialidade() : "Não classificado",
                        Collectors.counting()));

        List<DashboardResponse.DemandaPorEspecialidade> demanda = porEspecialidade.entrySet().stream()
                .map(e -> new DashboardResponse.DemandaPorEspecialidade(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(DashboardResponse.DemandaPorEspecialidade::totalAguardando).reversed())
                .toList();

        LocalDate inicioMesAtual = YearMonth.from(hoje).atDay(1);
        List<DashboardResponse.OcupacaoEspecialidade> ocupacao = calcularOcupacaoPorMes(inicioMesAtual);
        Integer taxaGeral = calcularTaxaGeral(ocupacao);

        // "Pico máximo" (item 3.4 do levantamento de requisitos): maior taxa de
        // ocupação geral já registrada entre todos os meses com cota
        // cadastrada, não só o mês corrente -- dá ao gestor uma referência de
        // pior caso histórico para planejamento de capacidade.
        Integer picoOcupacaoGeral = calcularPicoOcupacaoGeral();

        DashboardResponse.SlaResumo sla = calcularSla(aguardando, hoje);
        List<Protocolo> concluidos = protocoloRepository.findByStatus(StatusProtocolo.CONCLUIDO);
        List<DashboardResponse.TempoMedioEspecialidade> tempoMedio = calcularTempoMedioPorEspecialidade(aguardando, concluidos);
        DashboardResponse.ConfirmacaoPresencaResumo confirmacaoPresenca = calcularConfirmacaoPresenca();

        return new DashboardResponse(filaDeEspera, agendadosNoMes, realizadosNoAno, demanda,
                taxaGeral, picoOcupacaoGeral, ocupacao, sla, tempoMedio, confirmacaoPresenca);
    }

    /**
     * Entre os protocolos aguardando atendimento, quantos já ultrapassaram o
     * limite de dias considerado "Atrasado" -- mesmo critério da varredura
     * diária de alerta (ver SlaAlertaService) e do badge de prazo da fila.
     */
    private DashboardResponse.SlaResumo calcularSla(List<Protocolo> aguardando, LocalDate hoje) {
        LocalDate dataLimite = hoje.minusDays(diasLimiteAtrasado);
        long totalAguardando = aguardando.size();
        long totalAtrasado = protocoloRepository.countByStatusAndDataInclusaoLessThanEqual(
                StatusProtocolo.AGUARDANDO, dataLimite);
        long totalDentroPrazo = totalAguardando - totalAtrasado;
        Integer percentual = totalAguardando > 0
                ? (int) Math.round((totalDentroPrazo * 100.0) / totalAguardando)
                : null;
        return new DashboardResponse.SlaResumo(totalAguardando, totalDentroPrazo, totalAtrasado, percentual, diasLimiteAtrasado);
    }

    /**
     * Tempo médio de espera por especialidade (dois números complementares --
     * ver Javadoc de {@code DashboardResponse.TempoMedioEspecialidade}):
     * idade média do backlog ainda aguardando, e tempo médio até a conclusão
     * de quem já foi atendido. Reaproveita as listas já carregadas pelo
     * restante do painel (mesmo padrão do método, sem query extra).
     */
    private List<DashboardResponse.TempoMedioEspecialidade> calcularTempoMedioPorEspecialidade(
            List<Protocolo> aguardando, List<Protocolo> concluidos) {

        Map<String, Double> mediaEsperaAtual = aguardando.stream()
                .collect(Collectors.groupingBy(this::especialidadeDe, Collectors.averagingLong(Protocolo::diasEmEspera)));

        Map<String, Double> mediaAteConclusao = concluidos.stream()
                .collect(Collectors.groupingBy(this::especialidadeDe,
                        Collectors.averagingLong(p -> ChronoUnit.DAYS.between(p.getDataInclusao(), p.getAtualizadoEm().toLocalDate()))));

        return java.util.stream.Stream.concat(mediaEsperaAtual.keySet().stream(), mediaAteConclusao.keySet().stream())
                .distinct()
                .map(especialidade -> new DashboardResponse.TempoMedioEspecialidade(
                        especialidade, mediaEsperaAtual.get(especialidade), mediaAteConclusao.get(especialidade)))
                .sorted(Comparator.comparing(DashboardResponse.TempoMedioEspecialidade::especialidade))
                .toList();
    }

    private String especialidadeDe(Protocolo protocolo) {
        String especialidade = protocolo.getProcedimento().getEspecialidade();
        return especialidade != null ? especialidade : "Não classificado";
    }

    /**
     * Taxa de confirmação de presença (ver LembreteAgendamentoService/
     * ProtocoloConfirmacaoService): entre quem já recebeu o lembrete, quantos
     * confirmaram, cancelaram ou ainda não responderam.
     */
    private DashboardResponse.ConfirmacaoPresencaResumo calcularConfirmacaoPresenca() {
        long confirmados = protocoloRepository.countByPresencaConfirmacao(PresencaConfirmacao.CONFIRMADA);
        long cancelados = protocoloRepository.countByPresencaConfirmacao(PresencaConfirmacao.CANCELADA);
        long pendentes = protocoloRepository.countByPresencaConfirmacao(PresencaConfirmacao.PENDENTE);
        long totalRespondido = confirmados + cancelados;
        Integer percentual = totalRespondido > 0
                ? (int) Math.round((confirmados * 100.0) / totalRespondido)
                : null;
        return new DashboardResponse.ConfirmacaoPresencaResumo(
                confirmados + cancelados + pendentes, confirmados, cancelados, pendentes, percentual);
    }

    private Integer calcularTaxaGeral(List<DashboardResponse.OcupacaoEspecialidade> ocupacao) {
        if (ocupacao.isEmpty()) {
            return null;
        }
        long totalCotas = ocupacao.stream().mapToLong(DashboardResponse.OcupacaoEspecialidade::quantidadeTotal).sum();
        long totalUtilizado = ocupacao.stream().mapToLong(DashboardResponse.OcupacaoEspecialidade::quantidadeUtilizada).sum();
        return totalCotas > 0 ? (int) Math.round((totalUtilizado * 100.0) / totalCotas) : 0;
    }

    private Integer calcularPicoOcupacaoGeral() {
        List<LocalDate> meses = cotaRepository.findAll().stream()
                .map(Cota::getMesReferencia)
                .distinct()
                .toList();

        Integer pico = null;
        for (LocalDate mes : meses) {
            Integer taxaDoMes = calcularTaxaGeral(calcularOcupacaoPorMes(mes));
            if (taxaDoMes != null && (pico == null || taxaDoMes > pico)) {
                pico = taxaDoMes;
            }
        }
        return pico;
    }

    private List<DashboardResponse.OcupacaoEspecialidade> calcularOcupacaoPorMes(LocalDate mesReferencia) {
        LocalDate inicioMes = YearMonth.from(mesReferencia).atDay(1);
        LocalDate fimMes = YearMonth.from(mesReferencia).atEndOfMonth();

        List<Cota> cotasDoMes = cotaRepository.findByMesReferenciaOrderByUnidadeSaude_NomeAscEspecialidadeAsc(inicioMes);

        // Agrupa por especialidade, somando a cota e a utilização de todas as
        // unidades -- a "quantidade utilizada" nunca é um contador manual: é
        // calculada a partir dos protocolos reais (mesmo critério do CotaService),
        // para nunca ficar dessincronizada da fila real.
        Map<String, int[]> agregadoTotal = new LinkedHashMap<>(); // especialidade -> [quantidadeTotal]
        Map<String, long[]> agregadoUtilizado = new LinkedHashMap<>(); // especialidade -> [quantidadeUtilizada]

        for (Cota cota : cotasDoMes) {
            agregadoTotal.merge(cota.getEspecialidade(), new int[]{cota.getQuantidadeTotal()},
                    (a, b) -> new int[]{a[0] + b[0]});

            long utilizada = protocoloRepository
                    .findByUnidadeSaudeIdAndProcedimento_EspecialidadeAndDataInclusaoBetween(
                            cota.getUnidadeSaude().getId(), cota.getEspecialidade(), inicioMes, fimMes)
                    .stream()
                    .filter(p -> p.getStatus() != StatusProtocolo.CANCELADO)
                    .count();

            agregadoUtilizado.merge(cota.getEspecialidade(), new long[]{utilizada},
                    (a, b) -> new long[]{a[0] + b[0]});
        }

        return agregadoTotal.keySet().stream()
                .map(especialidade -> {
                    int total = agregadoTotal.get(especialidade)[0];
                    long utilizada = agregadoUtilizado.getOrDefault(especialidade, new long[]{0})[0];
                    int percentual = total > 0 ? (int) Math.round((utilizada * 100.0) / total) : 0;
                    return new DashboardResponse.OcupacaoEspecialidade(
                            especialidade, total, utilizada, percentual, percentual >= LIMIAR_GARGALO);
                })
                .sorted(Comparator.comparingInt(DashboardResponse.OcupacaoEspecialidade::percentual).reversed())
                .toList();
    }
}
