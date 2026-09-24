package br.com.filasaude.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Uma vaga concreta de agenda: unidade de saude + especialidade + data +
 * hora, com uma capacidade (quantos pacientes podem ser atendidos naquele
 * horario). E o que dá "hora real" ao agendamento -- antes disso, um
 * protocolo AGENDADO tinha só uma data solta (Protocolo.dataPrevista), sem
 * hora nem controle de quantas pessoas cabem ali.
 *
 * A quantidade ja ocupada não é armazenada aqui -- é calculada contando os
 * protocolos vinculados a este horário com status diferente de CANCELADO
 * (ver ProtocoloRepository/HorarioAgendaService), mesmo padrão usado pela
 * Cota mensal.
 */
@Entity
@Table(name = "horarios_agenda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_saude_id")
    private UnidadeSaude unidadeSaude;

    @Column(nullable = false, length = 100)
    private String especialidade;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "capacidade_total", nullable = false)
    private Integer capacidadeTotal;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
