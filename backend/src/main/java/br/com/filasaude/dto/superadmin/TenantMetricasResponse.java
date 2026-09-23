package br.com.filasaude.dto.superadmin;

/** Uso básico de um tenant — item 3.6: "monitorar todos os tenants, uso e suporte". */
public record TenantMetricasResponse(
        String slug,
        long totalUsuarios,
        long totalPacientes,
        long totalProtocolos,
        long protocolosAguardando
) {
}
