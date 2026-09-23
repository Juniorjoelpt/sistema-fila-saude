package br.com.filasaude.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cota de vagas por unidade de saude e especialidade, em um mes de
 * referencia (item 3.3 do levantamento de requisitos). A quantidade
 * efetivamente utilizada nao e armazenada aqui -- e calculada a partir dos
 * protocolos da fila (ver CotaService), para nunca ficar dessincronizada.
 */
@Entity
@Table(name = "cotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_saude_id")
    private UnidadeSaude unidadeSaude;

    @Column(nullable = false, length = 100)
    private String especialidade;

    /** Sempre normalizado para o dia 01 do mes (ex.: cota de setembro/2026 = 2026-09-01). */
    @Column(name = "mes_referencia", nullable = false)
    private LocalDate mesReferencia;

    @Column(name = "quantidade_total", nullable = false)
    private Integer quantidadeTotal;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
