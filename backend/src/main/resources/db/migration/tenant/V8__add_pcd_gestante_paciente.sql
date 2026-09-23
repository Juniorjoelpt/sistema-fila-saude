-- Suporta validação real da categoria de prioridade LEGAL (60+, PCD ou
-- gestante -- item 3.2 do levantamento de requisitos). Antes o sistema
-- aceitava a categoria ESPECIAL/LEGAL sem checar nenhum critério.
ALTER TABLE pacientes ADD COLUMN pcd BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pacientes ADD COLUMN gestante BOOLEAN NOT NULL DEFAULT FALSE;
