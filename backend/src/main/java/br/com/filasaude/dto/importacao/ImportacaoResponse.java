package br.com.filasaude.dto.importacao;

import java.util.List;

/** Relatório de uma importação em lote (item 3.4): total de linhas lidas, quantas foram importadas e os erros. */
public record ImportacaoResponse(
        int totalLinhas,
        int importados,
        List<ImportacaoErro> erros
) {
}
