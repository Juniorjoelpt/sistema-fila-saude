-- Marca quando o alerta de SLA vencido (prazo "Atrasado", item de melhoria pós-MVP)
-- foi enviado para um protocolo, para a varredura diária (SlaAlertaService) nunca
-- notificar o mesmo protocolo duas vezes. NULL = ainda não alertado.
ALTER TABLE protocolos ADD COLUMN alerta_sla_enviado_em DATETIME NULL;
