-- Transparência ativa (LAI): configuração da página pública de indicadores da fila.
-- Linha única (id = 1). Nasce DESLIGADA: a prefeitura só publica quando o secretário decidir.
CREATE TABLE transparencia_config (
    id                    BIGINT PRIMARY KEY,
    publicado             BOOLEAN NOT NULL DEFAULT FALSE,
    mostrar_fila          BOOLEAN NOT NULL DEFAULT TRUE,
    mostrar_atendimentos  BOOLEAN NOT NULL DEFAULT TRUE,
    mostrar_vagas         BOOLEAN NOT NULL DEFAULT TRUE,
    mostrar_criterios     BOOLEAN NOT NULL DEFAULT TRUE,
    texto_introducao      VARCHAR(1000) NULL,
    contato_lai           VARCHAR(300) NULL,
    atualizado_em         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_por_id     BIGINT NULL
);

INSERT INTO transparencia_config (id) VALUES (1);
