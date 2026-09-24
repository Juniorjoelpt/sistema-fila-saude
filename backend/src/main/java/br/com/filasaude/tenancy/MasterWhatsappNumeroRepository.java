package br.com.filasaude.tenancy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

/**
 * Acesso à tabela `whatsapp_numeros` no banco MASTER: mapeamento
 * phone_number_id -> tenant, usado pelo webhook único de respostas do
 * WhatsApp (ver WhatsappWebhookController) para descobrir a qual prefeitura
 * um evento recebido pertence, antes de resolver o TenantContext. Mesmo
 * padrão de {@link MasterTenantRepository} -- JdbcTemplate simples,
 * construído manualmente (não como @Bean) pelo mesmo motivo de ciclo de
 * beans documentado lá.
 */
@Repository
public class MasterWhatsappNumeroRepository {

    private final JdbcTemplate masterJdbcTemplate;

    public MasterWhatsappNumeroRepository(@Qualifier("masterDataSource") DataSource masterDataSource) {
        this.masterJdbcTemplate = new JdbcTemplate(masterDataSource);
    }

    /** Registra ou atualiza o dono de um phone_number_id -- chamado quando o admin ativa/edita a integração WHATSAPP. */
    public void registrar(String phoneNumberId, String tenantSlug) {
        masterJdbcTemplate.update(
                """
                INSERT INTO whatsapp_numeros (phone_number_id, tenant_slug, atualizado_em)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE tenant_slug = VALUES(tenant_slug), atualizado_em = CURRENT_TIMESTAMP
                """,
                phoneNumberId, tenantSlug
        );
    }

    /** Remove o mapeamento -- chamado quando o admin desativa a integração ou troca o número. */
    public void remover(String phoneNumberId) {
        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            return;
        }
        masterJdbcTemplate.update("DELETE FROM whatsapp_numeros WHERE phone_number_id = ?", phoneNumberId);
    }

    public Optional<String> buscarTenantSlug(String phoneNumberId) {
        List<String> resultado = masterJdbcTemplate.query(
                "SELECT tenant_slug FROM whatsapp_numeros WHERE phone_number_id = ?",
                (rs, rowNum) -> rs.getString("tenant_slug"),
                phoneNumberId
        );
        return resultado.stream().findFirst();
    }
}
