package br.com.filasaude.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pacientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 11, unique = true)
    private String cpf;

    @Column(length = 15, unique = true)
    private String cns;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(length = 20)
    private String telefone;

    @Column(length = 150)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acs_responsavel_id")
    private Usuario acsResponsavel;

    /**
     * Usados para validar de verdade a categoria de prioridade LEGAL (60+, PCD
     * ou gestante -- item 3.2 do levantamento de requisitos). Gap de revisão
     * corrigido: antes o operador escolhia ESPECIAL/LEGAL livremente no
     * cadastro do protocolo, sem o sistema checar nenhum critério real.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean pcd = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean gestante = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
