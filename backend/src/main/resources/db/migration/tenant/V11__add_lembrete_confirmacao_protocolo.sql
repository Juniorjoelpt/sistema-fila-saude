-- Lembrete de agendamento + confirmacao de presenca pelo cidadao (melhoria
-- pos-MVP sugerida apos o agendamento de horario real: hoje o paciente so
-- ficava sabendo da data/hora se lembrasse de consultar o protocolo, sem
-- nenhum lembrete proativo nem forma de avisar que nao vai comparecer).
--
-- confirmacao_token: identificador opaco (UUID) enviado por e-mail, usado
-- pela tela publica de confirmacao para localizar o protocolo sem exigir
-- login nem o CPF/CNS de novo -- e o mesmo padrao de "prova de posse do
-- link" ja usado em outros fluxos publicos deste sistema.
-- lembrete_enviado_em: evita reenvio duplicado do lembrete na varredura
-- diaria (mesmo padrao de alerta_sla_enviado_em).
-- presenca_confirmacao: PENDENTE ate o paciente clicar em confirmar/cancelar
-- pelo link; fica NULL para protocolos que nunca chegaram a ter um lembrete
-- enviado.
ALTER TABLE protocolos ADD COLUMN confirmacao_token VARCHAR(36) NULL;
ALTER TABLE protocolos ADD COLUMN lembrete_enviado_em TIMESTAMP NULL;
ALTER TABLE protocolos ADD COLUMN presenca_confirmacao VARCHAR(20) NULL;
ALTER TABLE protocolos ADD CONSTRAINT uk_protocolo_confirmacao_token UNIQUE (confirmacao_token);
