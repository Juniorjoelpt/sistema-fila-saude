-- Log de auditoria geral do sistema (item 3.5 do levantamento de
-- requisitos): registro amplo de ações administrativas -- login,
-- criação/edição de cadastros, pacientes, protocolos, usuários -- além do
-- que já existe especificamente para status de protocolo
-- (historico_status), prioridade (historico_prioridade) e cota
-- (cota_ajustes). Log imutável, apenas inserido.
CREATE TABLE log_auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NULL,
    usuario_email VARCHAR(150) NULL,
    acao VARCHAR(50) NOT NULL,
    entidade VARCHAR(50) NULL,
    entidade_id BIGINT NULL,
    detalhe VARCHAR(500) NULL,
    criado_em DATETIME NOT NULL,
    CONSTRAINT fk_log_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_log_auditoria_criado_em ON log_auditoria (criado_em);
CREATE INDEX idx_log_auditoria_acao ON log_auditoria (acao);
