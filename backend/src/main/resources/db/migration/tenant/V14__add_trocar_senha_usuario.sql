-- Senha provisória: usuário criado por um Admin (ou com senha redefinida por ele)
-- fica obrigado a trocar a senha no próximo acesso. Usuários já existentes
-- (inclusive os de demonstração) NÃO são afetados: o valor padrão é FALSE.
ALTER TABLE usuarios ADD COLUMN trocar_senha BOOLEAN NOT NULL DEFAULT FALSE;
