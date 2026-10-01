-- SLA configurável por especialidade/procedimento (roadmap "Ganhos rápidos").
--
-- Até aqui o limite de dias considerado "Atrasado" (e o de "Atenção" no
-- badge de prazo da fila) era um único valor fixo para o tenant inteiro
-- (propriedade filasaude.sla.dias-limite-atrasado, padrão 15 dias), o mesmo
-- para qualquer especialidade ou procedimento. Isso não reflete a realidade
-- de uma fila de regulação: uma consulta simples e uma cirurgia têm
-- parâmetros de tempo de espera muito diferentes.
--
-- Esta tabela guarda OVERRIDES opcionais desse limite padrão: uma linha por
-- especialidade (especialidade preenchida, procedimento_id nulo) ou uma
-- linha por procedimento específico (procedimento_id preenchido,
-- especialidade nula -- a especialidade do procedimento já é conhecida via
-- FK, não precisa ser repetida). Quando nenhum override existe para um
-- protocolo, o limite padrão do tenant continua valendo (ver
-- SlaConfiguracaoService#resolverLimites). Procedimento é mais específico
-- que especialidade: um override de procedimento vence um de especialidade.
CREATE TABLE sla_configuracoes (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    especialidade       VARCHAR(100) NULL,
    procedimento_id     BIGINT NULL,
    dias_atencao        INT NOT NULL,
    dias_atrasado       INT NOT NULL,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sla_configuracao_procedimento FOREIGN KEY (procedimento_id) REFERENCES procedimentos (id),
    -- NULL não conflita com NULL em índice único no MySQL, então múltiplas
    -- linhas de nível-procedimento (especialidade NULL) ou de nível só-com-
    -- especialidade-nula não geram falso conflito entre si.
    CONSTRAINT uq_sla_configuracao_especialidade UNIQUE (especialidade),
    CONSTRAINT uq_sla_configuracao_procedimento UNIQUE (procedimento_id),
    CONSTRAINT ck_sla_configuracao_escopo CHECK (
        (especialidade IS NOT NULL AND procedimento_id IS NULL)
        OR (especialidade IS NULL AND procedimento_id IS NOT NULL)
    ),
    CONSTRAINT ck_sla_configuracao_dias CHECK (dias_atencao > 0 AND dias_atrasado > dias_atencao)
);
