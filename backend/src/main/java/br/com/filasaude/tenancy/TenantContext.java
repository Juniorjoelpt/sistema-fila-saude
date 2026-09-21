package br.com.filasaude.tenancy;

/**
 * Mantém o identificador do tenant (prefeitura) corrente na thread da requisição.
 * Populado pelo {@link TenantResolverFilter} logo no início do ciclo de vida da requisição
 * e limpo ao final, para nunca vazar entre requisições (importante em pool de threads).
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
