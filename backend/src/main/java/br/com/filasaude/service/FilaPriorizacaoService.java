package br.com.filasaude.service;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.repository.ProtocoloRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Motor de priorizacao da fila (item 3.2 do levantamento de requisitos).
 *
 * Ordena os protocolos aguardando atendimento por: (1) peso da categoria de
 * prioridade — Urgencia > Judicial > Especial > Legal > Normal — e, dentro da
 * mesma categoria, (2) data de inclusao na fila (FIFO), garantindo a cronologia
 * auditavel exigida no item 3.5 (protecao contra "fura-filas").
 *
 * Centralizado aqui para que a mesma logica sirva tanto a consulta publica do
 * cidadao (posicao na fila) quanto a listagem administrativa da fila.
 */
@Service
public class FilaPriorizacaoService {

    private static final Comparator<Protocolo> ORDEM_DE_PRIORIDADE =
            Comparator.<Protocolo>comparingInt(p -> p.getCategoriaPrioridade().getPeso())
                    .thenComparing(Protocolo::getDataInclusao)
                    .thenComparing(Protocolo::getCriadoEm);

    private final ProtocoloRepository protocoloRepository;

    public FilaPriorizacaoService(ProtocoloRepository protocoloRepository) {
        this.protocoloRepository = protocoloRepository;
    }

    /** Fila de protocolos aguardando atendimento, já ordenada pelo motor de priorização. */
    public List<Protocolo> filaOrdenada() {
        List<Protocolo> aguardando = protocoloRepository.findByStatus(StatusProtocolo.AGUARDANDO);
        return aguardando.stream().sorted(ORDEM_DE_PRIORIDADE).toList();
    }

    /**
     * Posição (1-based) de um protocolo na fila do SEU procedimento, ou vazio
     * se ele não está aguardando. A posição é por procedimento: antes era
     * calculada sobre todos os protocolos aguardando, e um paciente de
     * Ortopedia aparecia como "3º" por causa de pacientes de Cardiologia que
     * nunca disputam a mesma vaga que ele.
     */
    public Optional<Integer> posicaoNaFila(Long protocoloId) {
        return Optional.ofNullable(posicoesPorProtocolo().get(protocoloId));
    }

    /**
     * Posição de TODOS os protocolos aguardando, calculada uma única vez (mapa
     * protocoloId -> posição na fila do próprio procedimento). Usado pelas
     * listagens, que antes refaziam a fila inteira para cada linha exibida.
     */
    public Map<Long, Integer> posicoesPorProtocolo() {
        Map<Long, List<Protocolo>> porProcedimento = filaOrdenada().stream()
                .collect(Collectors.groupingBy(p -> p.getProcedimento().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<Long, Integer> posicoes = new HashMap<>();
        for (List<Protocolo> fila : porProcedimento.values()) {
            for (int i = 0; i < fila.size(); i++) {
                posicoes.put(fila.get(i).getId(), i + 1);
            }
        }
        return posicoes;
    }
}
