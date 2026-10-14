-- Urgência/Judicial pedidas por um ACS nascem "pendentes de confirmação do regulador":
-- o protocolo entra na fila como NORMAL e guarda aqui a categoria solicitada.
-- NULL = nada pendente.
ALTER TABLE protocolos ADD COLUMN categoria_solicitada VARCHAR(20) NULL;
