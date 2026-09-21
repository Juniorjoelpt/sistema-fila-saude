package br.com.filasaude.service;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.dashboard.DashboardResponse;
import br.com.filasaude.repository.ProtocoloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Indicadores do painel administrativo municipal (item 3.4). A "taxa de
 * ocupação" da referência de mercado depende do módulo de cotas por unidade,
 * que fica para a fase 2 (recorte de MVP definido no levantamento de
 * requisitos) — por isso não aparece aqui ainda.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ProtocoloRepository protocoloRepository;

    public DashboardService(ProtocoloRepository protocoloRepository) {
        this.protocoloRepository = protocoloRepository;
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

        return new DashboardResponse(filaDeEspera, agendadosNoMes, realizadosNoAno, demanda);
    }
}
