package br.com.filasaude.dto.protocolo;

import br.com.filasaude.domain.enums.CategoriaPrioridade;
import br.com.filasaude.service.ClassificacaoRiscoService;

/**
 * Sugestão de categoria de prioridade (item do roadmap "classificação de
 * risco assistida"), com o motivo em texto para o operador entender a
 * recomendação -- a decisão final de categoria continua sendo dele, feita
 * no próprio formulário de Novo Protocolo / alteração de prioridade.
 */
public record ClassificacaoSugeridaResponse(
        CategoriaPrioridade categoriaSugerida,
        String descricaoCategoria,
        String motivo
) {
    public static ClassificacaoSugeridaResponse de(ClassificacaoRiscoService.ClassificacaoSugerida sugerida) {
        return new ClassificacaoSugeridaResponse(
                sugerida.categoriaSugerida(), sugerida.categoriaSugerida().getDescricao(), sugerida.motivo());
    }
}
