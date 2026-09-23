package br.com.filasaude.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Log de auditoria geral do sistema (item 3.5 do levantamento de
 * requisitos): registro amplo de ações administrativas -- login,
 * criação/edição de cadastros, pacientes, protocolos, usuários -- além do
 * que já existe especificamente para status/prioridade de protocolo e
 * ajuste de cota. Nunca é atualizado ou apagado, apenas inserido.
 *
 * O e-mail do usuário fica desnormalizado (usuarioEmail) para o registro
 * continuar legível mesmo que o usuário seja desativado/removido depois, e
 * para cobrir o caso de tentativa de login com credenciais inválidas (sem
 * usuário resolvido).
 */
@Entity
@Table(name = "log_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "usuario_email", length = 150)
    private String usuarioEmail;

    @Column(nullable = false, length = 50)
    private String acao;

    @Column(length = 50)
    private String entidade;

    @Column(name = "entidade_id")
    private Long entidadeId;

    @Column(length = 500)
    private String detalhe;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
