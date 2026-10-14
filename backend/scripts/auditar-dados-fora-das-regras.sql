-- Relatório SOMENTE LEITURA de registros que as regras atuais recusariam
-- (achado B3 do teste prático). Não altera nada: rode no banco do tenant,
--   mysql -h localhost -u root -p filasaude_demo < auditar-dados-fora-das-regras.sql
-- e corrija/remova os registros listados pela própria tela do sistema.

-- 1) Pacientes com CPF/CNS de tamanho errado (o sistema só aceita 11 / 15 dígitos)
SELECT id, nome, cpf, cns FROM pacientes
WHERE (cpf IS NOT NULL AND CHAR_LENGTH(cpf) <> 11)
   OR (cns IS NOT NULL AND CHAR_LENGTH(cns) <> 15);

-- 2) Pacientes sem data de nascimento (não conseguem consultar o portal público)
SELECT id, nome, cpf, cns FROM pacientes WHERE data_nascimento IS NULL;

-- 3) Protocolos Especial (80+) / Legal (60+, PCD, gestante) cujo paciente não atende ao critério
SELECT p.numero_protocolo, pac.nome, pac.data_nascimento, pac.pcd, pac.gestante, p.categoria_prioridade
FROM protocolos p JOIN pacientes pac ON pac.id = p.paciente_id
WHERE (p.categoria_prioridade = 'ESPECIAL'
        AND (pac.data_nascimento IS NULL OR TIMESTAMPDIFF(YEAR, pac.data_nascimento, CURDATE()) < 80))
   OR (p.categoria_prioridade = 'LEGAL'
        AND pac.pcd = false AND pac.gestante = false
        AND (pac.data_nascimento IS NULL OR TIMESTAMPDIFF(YEAR, pac.data_nascimento, CURDATE()) < 60));

-- 4) Procedimentos sem especialidade (não entram em cotas, agenda nem SLA por especialidade)
SELECT id, nome, tipo FROM procedimentos WHERE especialidade IS NULL OR especialidade = '';

-- 5) Procedimentos com nome repetido
SELECT LOWER(nome) AS nome, COUNT(*) AS quantidade FROM procedimentos GROUP BY LOWER(nome) HAVING COUNT(*) > 1;

-- 6) Números de protocolo fora do formato atual PROT-AAAA-NNNNNN
SELECT id, numero_protocolo FROM protocolos WHERE numero_protocolo NOT REGEXP '^PROT-[0-9]{4}-[0-9]{6}$';

-- 7) Protocolos agendados sem data prevista (agendados "no seco" por versões antigas)
SELECT numero_protocolo, status FROM protocolos WHERE status = 'AGENDADO' AND data_prevista IS NULL;

-- 8) Protocolos concluídos com etapas ainda abertas (versões antigas não fechavam as etapas)
SELECT DISTINCT p.numero_protocolo FROM protocolos p
JOIN etapas_protocolo e ON e.protocolo_id = p.id
WHERE p.status = 'CONCLUIDO' AND e.status <> 'REALIZADO';
