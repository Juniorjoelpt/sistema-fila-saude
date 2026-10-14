package br.com.filasaude.dto.transparencia;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Indicadores públicos (transparência ativa / LAI) -- SEMPRE agregados. Nenhum
 * campo identifica paciente: nada de nome, CPF, CNS ou número de protocolo.
 * Contagens de 1 a (limiteSupressao - 1) pessoas saem como "menos de N" para
 * ninguém ser identificado por dedução (LGPD). Seções desligadas pelo
 * secretário vêm nulas.
 */
public record TransparenciaResponse(
        LocalDateTime geradoEm,
        String textoIntroducao,
        String contatoLai,
        int limiteSupressao,
        Fila fila,
        List<AtendimentoMes> atendimentos,
        List<VagasUnidade> vagas,
        List<CriterioFila> criterios
) {

    /** Contagem publicável: {@code valor} é null quando suprimido; {@code rotulo} é o texto a exibir. */
    public record Contagem(Integer valor, String rotulo) {
        public static Contagem de(long quantidade, int limiteSupressao) {
            if (quantidade > 0 && quantidade < limiteSupressao) {
                return new Contagem(null, "menos de " + limiteSupressao);
            }
            return new Contagem((int) quantidade, String.valueOf(quantidade));
        }
    }

    public record Fila(
            Contagem totalAguardando,
            /** % dentro do prazo / em atenção / fora do prazo; null quando o total é pequeno demais para publicar. */
            Integer percentualDentroDoPrazo,
            Integer percentualEmAtencao,
            Integer percentualForaDoPrazo,
            List<FilaProcedimento> porProcedimento
    ) {
    }

    public record FilaProcedimento(
            String procedimento,
            String especialidade,
            Contagem aguardando,
            /** Espera média e máxima (dias) dos que aguardam; null em grupos pequenos. */
            Integer esperaMediaDias,
            Integer esperaMaximaDias,
            /** Estimativa de dias para esvaziar a fila no ritmo recente; null sem base ou em grupos pequenos. */
            Integer diasParaEsvaziarFila
    ) {
    }

    public record AtendimentoMes(String mes, Contagem concluidos) {
    }

    public record VagasUnidade(String unidade, long vagasOfertadas, String periodo) {
    }

    public record CriterioFila(int ordem, String categoria, String descricao) {
    }
}
