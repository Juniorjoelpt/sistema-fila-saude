package br.com.filasaude.domain.enums;

/**
 * Integrações com sistemas externos, configuráveis por tenant na tela de
 * Integrações (restrita a Admin): e-SUS, SISREG e CNES são obrigatórias
 * (confirmadas no levantamento de requisitos -- Decisões Confirmadas /
 * item 3 e seção "Já definido"), com dados do Ministério da Saúde. WHATSAPP
 * é opcional (melhoria pós-MVP): canal alternativo ao e-mail para o
 * lembrete de agendamento (ver LembreteAgendamentoTenantProcessor), via
 * WhatsApp Business Platform (Cloud API da Meta) -- cada prefeitura conecta
 * sua própria conta comercial, com "base_url" guardando o Phone Number ID e
 * "token" o access token da API.
 */
public enum TipoIntegracao {
    ESUS,
    SISREG,
    CNES,
    WHATSAPP
}
