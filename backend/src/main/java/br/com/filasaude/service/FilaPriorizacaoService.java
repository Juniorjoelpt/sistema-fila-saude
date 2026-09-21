package br.com.filasaude.service;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.repository.ProtocoloRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    /** Posição (1-based) de um protocolo na fila ordenada, ou vazio se ele não está aguardando. */
    public Optional<Integer> posicaoNaFila(Long protocoloId) {
        List<Protocolo> fila = filaOrdenada();
        for (int i = 0; i < fila.size(); i++) {
            if (fila.get(i).getId().equals(protocoloId)) {
                return Optional.of(i + 1);
            }
        }
        return Optional.empty();
    }
}
