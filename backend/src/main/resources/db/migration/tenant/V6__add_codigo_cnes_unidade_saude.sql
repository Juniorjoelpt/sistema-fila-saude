-- Código CNES (Cadastro Nacional de Estabelecimentos de Saúde) da unidade,
-- usado para vincular o cadastro local à integração com o CNES (item de
-- integrações obrigatórias do levantamento de requisitos).
ALTER TABLE unidades_saude ADD COLUMN codigo_cnes VARCHAR(20) NULL;
