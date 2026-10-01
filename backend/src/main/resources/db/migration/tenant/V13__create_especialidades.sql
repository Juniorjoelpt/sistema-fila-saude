-- Cadastro de especialidades (gap de revisão corrigido): até aqui a
-- "especialidade" era digitada como texto livre em cada tela que a usa
-- (Procedimentos, Cotas, Agenda de horários, SLA configurável) -- um erro
-- de digitação em qualquer uma delas (ex.: "Cardiologia " com espaço,
-- "cardiologia" vs "Cardiologia") quebra silenciosamente o casamento entre
-- essas telas, já que todas comparam a especialidade por igualdade de
-- string. Esta tabela passa a ser a fonte única: as demais telas selecionam
-- a partir daqui em vez de digitar.
CREATE TABLE especialidades (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(100) NOT NULL UNIQUE,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Semeia o cadastro com as especialidades já em uso em procedimentos,
-- cotas, horários de agenda e configurações de SLA -- sem isso, todo
-- cadastro existente ficaria com uma especialidade "órfã" (fora do
-- cadastro) no primeiro acesso após esta migração.
INSERT INTO especialidades (nome)
SELECT DISTINCT especialidade FROM (
    SELECT especialidade FROM procedimentos WHERE especialidade IS NOT NULL AND especialidade <> ''
    UNION
    SELECT especialidade FROM cotas WHERE especialidade IS NOT NULL AND especialidade <> ''
    UNION
    SELECT especialidade FROM horarios_agenda WHERE especialidade IS NOT NULL AND especialidade <> ''
    UNION
    SELECT especialidade FROM sla_configuracoes WHERE especialidade IS NOT NULL AND especialidade <> ''
) existentes;
