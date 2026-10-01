package br.com.filasaude.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Override do limite de dias de SLA (item do roadmap "SLA configurável por
 * especialidade/procedimento") para uma especialidade ou um procedimento
 * específico. Exatamente um dos dois ({@code especialidade} ou
 * {@code procedimento}) é preenchido -- ver constraint
 * {@code ck_sla_configuracao_escopo} na migração e validação em
 * {@code SlaConfiguracaoService}. Quando nenhuma linha cobre um protocolo, o
 * limite padrão do tenant (propriedade {@code filasaude.sla.*}) é usado.
 */
@Entity
@Table(name = "sla_configuracoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaConfiguracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String especialidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedimento_id")
    private Procedimento procedimento;

    @Column(name = "dias_atencao", nullable = false)
    private int diasAtencao;

    @Column(name = "dias_atrasado", nullable = false)
    private int diasAtrasado;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
