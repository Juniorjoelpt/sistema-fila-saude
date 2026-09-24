-- Mapeamento phone_number_id (WhatsApp Business) -> tenant, no banco MASTER.
--
-- Motivo de ficar no master, e não no banco de cada tenant: o webhook de
-- respostas do WhatsApp (ver WhatsappWebhookController) é UM ÚNICO endpoint
-- HTTP compartilhado por todas as prefeituras (a Meta manda todo evento para
-- a mesma URL, de um único "app" gerenciado pela MS Soluções). Quando esse
-- evento chega, ainda não sabemos qual tenant ele pertence -- o
-- TenantResolverFilter normal não se aplica aqui (a Meta não manda
-- X-Tenant-Id nem bate num subdomínio de prefeitura). Por isso o webhook
-- primeiro consulta ESTA tabela pelo phone_number_id (que vem no corpo do
-- evento) para descobrir o tenant, e só então resolve o TenantContext
-- manualmente e processa o evento no banco daquele tenant -- mesmo padrão já
-- usado pelas varreduras agendadas (SlaAlertaService/LembreteAgendamentoService).
--
-- Mantida em sincronia com IntegracaoConfig (tenant) sempre que o admin salva
-- ou desativa a integração WHATSAPP -- ver IntegracaoConfigService.
CREATE TABLE whatsapp_numeros (
    phone_number_id  VARCHAR(50) PRIMARY KEY,
    tenant_slug       VARCHAR(60) NOT NULL,
    atualizado_em     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT = 'Mapeamento phone_number_id -> tenant, usado pelo webhook único de respostas do WhatsApp.';
