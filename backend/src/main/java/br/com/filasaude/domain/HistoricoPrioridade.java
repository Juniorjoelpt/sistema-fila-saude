package br.com.filasaude.domain;

import br.com.filasaude.domain.enums.CategoriaPrioridade;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Log de auditoria imutável de reclassificações de prioridade de um
 * protocolo (item 3.5: cronologia auditável, proteção contra "fura-filas" e
 * contra questionamentos éticos/judiciais ao gestor). Nunca é atualizado ou
 * apagado, apenas inserido -- espelha o padrão de HistoricoStatus.
 */
@Entity
@Table(name = "historico_prioridade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoPrioridade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protocolo_id")
    private Protocolo protocolo;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade_anterior", nullable = false, length = 20)
    private CategoriaPrioridade prioridadeAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade_nova", nullable = false, length = 20)
    private CategoriaPrioridade prioridadeNova;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
