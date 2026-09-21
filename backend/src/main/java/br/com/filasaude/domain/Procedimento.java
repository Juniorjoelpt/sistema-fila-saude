package br.com.filasaude.domain;

import br.com.filasaude.domain.enums.TipoProcedimento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "procedimentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Procedimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoProcedimento tipo;

    @Column(length = 100)
    private String especialidade;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;
}
