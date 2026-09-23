-- Estrutura de configuração para as integrações obrigatórias com sistemas
-- do Ministério da Saúde (decisão confirmada no levantamento de
-- requisitos): e-SUS, SISREG e CNES. Cada tenant (prefeitura) guarda suas
-- próprias credenciais, já que cada município se conecta com sua própria
-- instância/registro nesses sistemas.
--
-- Enquanto não configurado (token nulo ou ativo=false), o sistema opera
-- normalmente sem a integração -- ela nunca é obrigatória para o uso do
-- produto, só um recurso adicional quando o cliente tiver acesso oficial
-- às APIs do Ministério da Saúde.
CREATE TABLE integracoes_config (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo                VARCHAR(20) NOT NULL UNIQUE, -- ESUS, SISREG, CNES
    base_url            VARCHAR(255),
    token               VARCHAR(500), -- credencial/token de acesso à API oficial
    ativo               BOOLEAN NOT NULL DEFAULT FALSE,
    atualizado_em       DATETIME NOT NULL,
    atualizado_por_id   BIGINT NULL,
    CONSTRAINT fk_integracoes_config_usuario FOREIGN KEY (atualizado_por_id) REFERENCES usuarios (id)
);
