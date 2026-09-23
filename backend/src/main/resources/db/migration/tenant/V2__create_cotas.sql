-- Gestao de cotas por unidade de saude e especialidade (item 3.3, Fase 2).

CREATE TABLE cotas (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    unidade_saude_id    BIGINT NOT NULL,
    especialidade       VARCHAR(100) NOT NULL,
    mes_referencia      DATE NOT NULL, -- sempre normalizado para o dia 01 do mes
    quantidade_total    INT NOT NULL,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cota_unidade FOREIGN KEY (unidade_saude_id) REFERENCES unidades_saude(id),
    CONSTRAINT uq_cota_unidade_especialidade_mes UNIQUE (unidade_saude_id, especialidade, mes_referencia)
);

CREATE INDEX idx_cotas_unidade ON cotas(unidade_saude_id);
CREATE INDEX idx_cotas_mes ON cotas(mes_referencia);

-- Historico de ajustes: quem alterou a cota e quando (item 3.3 - "registro de
-- quem ajustou e quando").
CREATE TABLE cota_ajustes (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    cota_id                 BIGINT NOT NULL,
    quantidade_anterior     INT NOT NULL,
    quantidade_nova         INT NOT NULL,
    usuario_id              BIGINT,
    motivo                  VARCHAR(300),
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ajuste_cota FOREIGN KEY (cota_id) REFERENCES cotas(id) ON DELETE CASCADE,
    CONSTRAINT fk_ajuste_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_cota_ajustes_cota ON cota_ajustes(cota_id);
