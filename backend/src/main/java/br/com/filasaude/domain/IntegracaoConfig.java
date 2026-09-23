package br.com.filasaude.domain;

import br.com.filasaude.domain.enums.TipoIntegracao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Configuração (por tenant) de uma integração obrigatória com sistemas do
 * Ministério da Saúde -- e-SUS, SISREG ou CNES. O token é a credencial de
 * acesso à API oficial daquele sistema; enquanto ausente ou "ativo=false",
 * a integração fica desligada e o sistema opera normalmente sem ela.
 *
 * O token NUNCA é devolvido em texto puro pela API (ver
 * IntegracaoConfigResponse) -- só indicamos se está configurado ou não.
 */
@Entity
@Table(name = "integracoes_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegracaoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, unique = true)
    private TipoIntegracao tipo;

    @Column(name = "base_url", length = 255)
    private String baseUrl;

    @Column(length = 500)
    private String token;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = false;

    @Column(name = "atualizado_em", nullable = false)
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por_id")
    private Usuario atualizadoPor;
}
