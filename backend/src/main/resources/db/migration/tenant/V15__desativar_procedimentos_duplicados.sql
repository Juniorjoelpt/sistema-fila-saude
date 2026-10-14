-- Procedimentos com o mesmo nome (sem diferenciar maiúsculas/minúsculas) cadastrados
-- antes da validação de nome duplicado: mantém o mais antigo ativo e desativa os
-- repetidos que ainda não têm nenhum protocolo. Repetidos já usados em protocolos são
-- preservados (o histórico não pode ficar sem referência) e aparecem no script
-- backend/scripts/auditar-dados-fora-das-regras.sql para decisão manual.
UPDATE procedimentos p
JOIN procedimentos o
  ON LOWER(TRIM(o.nome)) = LOWER(TRIM(p.nome)) AND o.id < p.id AND o.ativo = TRUE
SET p.ativo = FALSE
WHERE p.ativo = TRUE
  AND NOT EXISTS (SELECT 1 FROM protocolos x WHERE x.procedimento_id = p.id);
