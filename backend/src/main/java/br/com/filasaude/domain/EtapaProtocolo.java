package br.com.filasaude.domain;

import br.com.filasaude.domain.enums.StatusEtapa;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Uma etapa da linha do tempo do protocolo (ex.: "Consulta Pre-operatoria",
 * "Risco Cirurgico"...), exibida ao cidadao na consulta publica (item 3.1).
 */
@Entity
@Table(name = "etapas_protocolo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapaProtocolo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protocolo_id")
    @JsonBackReference
    private Protocolo protocolo;

    @Column(name = "nome_etapa", nullable = false, length = 100)
    private String nomeEtapa;

    @Column(nullable = false)
    private Integer ordem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusEtapa status = StatusEtapa.AGUARDANDO;

    @Column(name = "data_realizacao")
    private LocalDate dataRealizacao;
}
