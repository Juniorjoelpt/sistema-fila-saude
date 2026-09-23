-- Auditoria de reclassificação de prioridade de protocolos (item 3.5 do
-- levantamento de requisitos: cronologia auditável, proteção contra
-- "fura-filas" e contra questionamentos éticos/judiciais ao gestor).
-- Espelha o padrão já usado em historico_status e cota_ajustes: log
-- imutável, nunca atualizado ou apagado, apenas inserido.
CREATE TABLE historico_prioridade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    protocolo_id BIGINT NOT NULL,
    prioridade_anterior VARCHAR(20) NOT NULL,
    prioridade_nova VARCHAR(20) NOT NULL,
    usuario_id BIGINT NULL,
    motivo VARCHAR(500) NOT NULL,
    criado_em DATETIME NOT NULL,
    CONSTRAINT fk_historico_prioridade_protocolo FOREIGN KEY (protocolo_id) REFERENCES protocolos (id),
    CONSTRAINT fk_historico_prioridade_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_historico_prioridade_protocolo ON historico_prioridade (protocolo_id);
