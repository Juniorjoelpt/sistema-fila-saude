-- Correções OPCIONAIS de dados legados (rode antes `auditar-dados-fora-das-regras.sql`
-- para ver o que será afetado). Faça backup do schema do tenant antes de executar.
-- Uso: mysql -u root filasaude_<tenant> < corrigir-dados-legados.sql

-- 1) Protocolos concluídos com etapas abertas: fecha as etapas, datando-as com a
--    data da conclusão (atualizado_em). Versões novas já fazem isso ao concluir.
UPDATE etapas_protocolo e
JOIN protocolos p ON p.id = e.protocolo_id
SET e.status = 'REALIZADO',
    e.data_realizacao = COALESCE(e.data_realizacao, DATE(p.atualizado_em))
WHERE p.status = 'CONCLUIDO' AND e.status <> 'REALIZADO';

-- 2) Etapas realizadas "fora de ordem" em protocolos abertos (revisão manual):
--    lista etapas REALIZADO que têm uma etapa anterior ainda pendente.
SELECT p.numero_protocolo, e.nome_etapa, e.ordem
FROM etapas_protocolo e
JOIN protocolos p ON p.id = e.protocolo_id
WHERE p.status IN ('AGUARDANDO', 'AGENDADO', 'EM_ANDAMENTO')
  AND e.status = 'REALIZADO'
  AND EXISTS (SELECT 1 FROM etapas_protocolo a
              WHERE a.protocolo_id = e.protocolo_id AND a.ordem < e.ordem AND a.status <> 'REALIZADO');
