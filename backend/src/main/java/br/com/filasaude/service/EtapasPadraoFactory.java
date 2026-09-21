package br.com.filasaude.service;

import br.com.filasaude.domain.EtapaProtocolo;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.TipoProcedimento;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Gera a linha do tempo (etapas) padrão de um protocolo, de acordo com o tipo de
 * procedimento — ex.: o ciclo cirúrgico de 5 etapas mostrado na referência de
 * mercado analisada (item 3.1: Consulta Pré-operatória → Risco Cirúrgico →
 * Consulta Pré-anestésica → Consulta Pré-operatória Final → Cirurgia Agendada).
 */
@Component
public class EtapasPadraoFactory {

    public List<EtapaProtocolo> gerarPara(Protocolo protocolo, TipoProcedimento tipo) {
        List<String> nomes = switch (tipo) {
            case CIRURGIA -> List.of(
                    "Consulta Pré-operatória",
                    "Risco Cirúrgico",
                    "Consulta Pré-anestésica",
                    "Consulta Pré-operatória Final",
                    "Cirurgia Agendada"
            );
            case EXAME -> List.of(
                    "Solicitação do Exame",
                    "Realização do Exame",
                    "Resultado Disponível"
            );
            case CONSULTA -> List.of(
                    "Solicitação da Consulta",
                    "Consulta Agendada"
            );
        };

        List<EtapaProtocolo> etapas = new ArrayList<>();
        for (int i = 0; i < nomes.size(); i++) {
            etapas.add(EtapaProtocolo.builder()
                    .protocolo(protocolo)
                    .nomeEtapa(nomes.get(i))
                    .ordem(i + 1)
                    .build());
        }
        return etapas;
    }
}
