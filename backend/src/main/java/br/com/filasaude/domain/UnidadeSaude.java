package br.com.filasaude.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "unidades_saude")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnidadeSaude {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 255)
    private String endereco;

    private Double latitude;

    private Double longitude;

    /** Código CNES (Cadastro Nacional de Estabelecimentos de Saúde), para a integração com o CNES. */
    @Column(name = "codigo_cnes", length = 20)
    private String codigoCnes;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;
}
