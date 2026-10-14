package br.com.filasaude.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Configuração (por tenant, linha única de id = 1) da página pública de
 * transparência ativa. Enquanto {@code publicado} for false, a página pública
 * responde 404 -- a prefeitura publica quando e o que decidir.
 */
@Entity
@Table(name = "transparencia_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransparenciaConfig {

    public static final long ID_UNICO = 1L;

    @Id
    @Builder.Default
    private Long id = ID_UNICO;

    @Column(nullable = false)
    private boolean publicado;

    @Column(name = "mostrar_fila", nullable = false)
    @Builder.Default
    private boolean mostrarFila = true;

    @Column(name = "mostrar_atendimentos", nullable = false)
    @Builder.Default
    private boolean mostrarAtendimentos = true;

    @Column(name = "mostrar_vagas", nullable = false)
    @Builder.Default
    private boolean mostrarVagas = true;

    @Column(name = "mostrar_criterios", nullable = false)
    @Builder.Default
    private boolean mostrarCriterios = true;

    @Column(name = "texto_introducao", length = 1000)
    private String textoIntroducao;

    @Column(name = "contato_lai", length = 300)
    private String contatoLai;

    @Column(name = "atualizado_em", nullable = false)
    @Builder.Default
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por_id")
    private Usuario atualizadoPor;
}
