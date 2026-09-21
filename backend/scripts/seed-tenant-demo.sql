-- Dados de exemplo para o tenant "demo" (banco filasaude_demo).
-- Rodar DEPOIS de disparar pelo menos uma requisição com o header
-- "X-Tenant-Id: demo" (isso aciona a migração Flyway sob demanda do schema):
--   mysql -h localhost -u root -proot filasaude_demo < seed-tenant-demo.sql

-- Usuário admin: e-mail admin@demo.filasaude.com.br / senha "admin123"
INSERT INTO usuarios (nome, email, senha_hash, papel, ativo)
VALUES ('Administrador Demo', 'admin@demo.filasaude.com.br',
        '$2b$12$UbierEBa8wnehrSWfMyYSe747bE/7WeiSYKpwjIUq5hnK2XJA2HV6', 'ADMIN', true);

INSERT INTO usuarios (nome, email, senha_hash, papel, ativo)
VALUES ('Teresinha ACS', 'teresinha@demo.filasaude.com.br',
        '$2b$12$UbierEBa8wnehrSWfMyYSe747bE/7WeiSYKpwjIUq5hnK2XJA2HV6', 'ACS', true);

INSERT INTO unidades_saude (nome, endereco, ativo)
VALUES ('UBS Centro', 'Rua Principal, 100 - Centro', true),
       ('Hospital Municipal', 'Av. da Saúde, 500', true);

INSERT INTO procedimentos (nome, tipo, especialidade, ativo)
VALUES ('Cirurgia de Catarata', 'CIRURGIA', 'Oftalmologia', true),
       ('Ecocardiograma', 'EXAME', 'Cardiologia', true),
       ('Consulta Clínico Geral', 'CONSULTA', 'Clínica Geral', true),
       ('Cirurgia Ortopédica', 'CIRURGIA', 'Ortopedia', true);

INSERT INTO pacientes (nome, cpf, data_nascimento, telefone, email, acs_responsavel_id)
VALUES
    ('José Eduardo Silva Marinho', '11122233344', '1950-03-12', '86999990001', 'jose.marinho@example.com',
        (SELECT id FROM usuarios WHERE email = 'teresinha@demo.filasaude.com.br')),
    ('Marcos Silva', '22233344455', '1965-07-20', '86999990002', 'marcos.silva@example.com', NULL),
    ('Ana Lima', '33344455566', '1980-01-05', '86999990003', 'ana.lima@example.com', NULL),
    ('Paulo Gomes', '44455566677', '1958-11-30', '86999990004', 'paulo.gomes@example.com', NULL);

-- Protocolo com categoria ESPECIAL (paciente 80+), aguardando
INSERT INTO protocolos (numero_protocolo, paciente_id, procedimento_id, unidade_saude_id,
                         categoria_prioridade, status, data_solicitacao, data_inclusao)
VALUES ('PROT-2026-0001',
        (SELECT id FROM pacientes WHERE cpf = '11122233344'),
        (SELECT id FROM procedimentos WHERE nome = 'Cirurgia Ortopédica'),
        (SELECT id FROM unidades_saude WHERE nome = 'Hospital Municipal'),
        'ESPECIAL', 'AGUARDANDO', '2026-02-13', '2026-02-24');

-- Protocolo NORMAL
INSERT INTO protocolos (numero_protocolo, paciente_id, procedimento_id, unidade_saude_id,
                         categoria_prioridade, status, data_solicitacao, data_inclusao)
VALUES ('PROT-2026-0002',
        (SELECT id FROM pacientes WHERE cpf = '22233344455'),
        (SELECT id FROM procedimentos WHERE nome = 'Cirurgia Ortopédica'),
        (SELECT id FROM unidades_saude WHERE nome = 'Hospital Municipal'),
        'NORMAL', 'AGUARDANDO', '2026-02-14', '2026-02-15');

-- Protocolo URGENCIA
INSERT INTO protocolos (numero_protocolo, paciente_id, procedimento_id, unidade_saude_id,
                         categoria_prioridade, status, data_solicitacao, data_inclusao)
VALUES ('PROT-2026-0003',
        (SELECT id FROM pacientes WHERE cpf = '33344455566'),
        (SELECT id FROM procedimentos WHERE nome = 'Ecocardiograma'),
        (SELECT id FROM unidades_saude WHERE nome = 'UBS Centro'),
        'URGENCIA', 'AGUARDANDO', '2026-02-27', '2026-03-01');

-- Protocolo LEGAL
INSERT INTO protocolos (numero_protocolo, paciente_id, procedimento_id, unidade_saude_id,
                         categoria_prioridade, status, data_solicitacao, data_inclusao)
VALUES ('PROT-2026-0004',
        (SELECT id FROM pacientes WHERE cpf = '44455566677'),
        (SELECT id FROM procedimentos WHERE nome = 'Consulta Clínico Geral'),
        (SELECT id FROM unidades_saude WHERE nome = 'UBS Centro'),
        'LEGAL', 'AGUARDANDO', '2026-01-23', '2026-01-30');

-- Etapas do ciclo cirurgico do protocolo PROT-2026-0001 (paciente José Eduardo),
-- espelhando o exemplo mostrado na referencia de mercado analisada.
INSERT INTO etapas_protocolo (protocolo_id, nome_etapa, ordem, status, data_realizacao)
SELECT id, 'Consulta Pré-operatória', 1, 'REALIZADO', '2026-02-15' FROM protocolos WHERE numero_protocolo = 'PROT-2026-0001';
INSERT INTO etapas_protocolo (protocolo_id, nome_etapa, ordem, status, data_realizacao)
SELECT id, 'Risco Cirúrgico', 2, 'REALIZADO', '2026-02-20' FROM protocolos WHERE numero_protocolo = 'PROT-2026-0001';
INSERT INTO etapas_protocolo (protocolo_id, nome_etapa, ordem, status, data_realizacao)
SELECT id, 'Consulta Pré-anestésica', 3, 'EM_ANDAMENTO', NULL FROM protocolos WHERE numero_protocolo = 'PROT-2026-0001';
INSERT INTO etapas_protocolo (protocolo_id, nome_etapa, ordem, status, data_realizacao)
SELECT id, 'Consulta Pré-operatória Final', 4, 'AGUARDANDO', NULL FROM protocolos WHERE numero_protocolo = 'PROT-2026-0001';
INSERT INTO etapas_protocolo (protocolo_id, nome_etapa, ordem, status, data_realizacao)
SELECT id, 'Cirurgia Agendada', 5, 'AGUARDANDO', NULL FROM protocolos WHERE numero_protocolo = 'PROT-2026-0001';
