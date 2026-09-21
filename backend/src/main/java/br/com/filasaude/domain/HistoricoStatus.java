package br.com.filasaude.domain;

import br.com.filasaude.domain.enums.StatusProtocolo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Log de auditoria imutavel de mudancas de status de um protocolo (item 3.5:
 * cronologia auditavel, protecao contra "fura-filas" e contra questionamentos
 * eticos/judiciais ao gestor). Nunca é atualizado ou apagado, apenas inserido.
 */
@Entity
@Table(name = "historico_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protocolo_id")
    private Protocolo protocolo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusProtocolo statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusProtocolo statusNovo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(length = 500)
    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
