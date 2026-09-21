package br.com.filasaude.domain.enums;

/**
 * Categorias de priorizacao da fila, na ordem definida no levantamento de requisitos (item 3.2):
 * Urgencia > Judicial > Especial (80+) > Legal (60+, PCD, gestante) > Normal (ordem cronologica).
 *
 * O campo `peso` e o criterio de ordenacao usado pelo motor de fila: quanto menor o peso,
 * mais prioritario. Dentro da mesma categoria, o desempate e por data de solicitacao (FIFO),
 * o que garante a cronologia auditavel exigida no item 3.5 (protecao contra "fura-filas").
 */
public enum CategoriaPrioridade {

    URGENCIA(1, "Urgência"),
    JUDICIAL(2, "Judicial"),
    ESPECIAL(3, "Especial (80+)"),
    LEGAL(4, "Legal (60+, PCD, gestante)"),
    NORMAL(5, "Normal");

    private final int peso;
    private final String descricao;

    CategoriaPrioridade(int peso, String descricao) {
        this.peso = peso;
        this.descricao = descricao;
    }

    public int getPeso() {
        return peso;
    }

    public String getDescricao() {
        return descricao;
    }
}
