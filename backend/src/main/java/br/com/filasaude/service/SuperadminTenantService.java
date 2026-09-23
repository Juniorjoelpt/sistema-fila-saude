package br.com.filasaude.service;

import br.com.filasaude.dto.superadmin.NovoTenantRequest;
import br.com.filasaude.dto.superadmin.TenantBrandingRequest;
import br.com.filasaude.dto.superadmin.TenantMetricasResponse;
import br.com.filasaude.dto.superadmin.TenantProvisionadoResponse;
import br.com.filasaude.dto.superadmin.TenantResumoResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.tenancy.MasterTenantRepository;
import br.com.filasaude.tenancy.TenantDataSourceRegistry;
import br.com.filasaude.tenancy.TenantRecord;
import com.zaxxer.hikari.HikariConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provisionamento self-service de novo tenant e monitoramento básico de uso
 * — item 3.6 do levantamento de requisitos (Fase 2, painel de superadmin).
 *
 * Provisionar uma prefeitura nova envolve, nesta ordem: criar o banco MySQL
 * físico dela, registrar o tenant no banco master, e então deixar o
 * mecanismo já existente de {@link TenantDataSourceRegistry} aplicar as
 * migrações Flyway na primeira vez que o tenant é acessado (aqui, forçado
 * imediatamente, para já criar o primeiro usuário admin).
 */
@Service
public class SuperadminTenantService {

    private static final Logger log = LoggerFactory.getLogger(SuperadminTenantService.class);
    private static final Pattern JDBC_HOST_PORT = Pattern.compile("jdbc:mysql://([^:/]+):(\\d+)/");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> TIPOS_LOGO_ACEITOS = Set.of("image/png");
    private static final long TAMANHO_MAXIMO_LOGO = 5L * 1024 * 1024; // 5MB

    private final MasterTenantRepository masterTenantRepository;
    private final TenantDataSourceRegistry tenantDataSourceRegistry;
    private final DataSource masterDataSource;
    private final HikariConfig masterHikariConfig;
    private final PasswordEncoder passwordEncoder;

    public SuperadminTenantService(MasterTenantRepository masterTenantRepository,
                                    TenantDataSourceRegistry tenantDataSourceRegistry,
                                    @Qualifier("masterDataSource") DataSource masterDataSource,
                                    @Qualifier("masterHikariConfig") HikariConfig masterHikariConfig,
                                    PasswordEncoder passwordEncoder) {
        this.masterTenantRepository = masterTenantRepository;
        this.tenantDataSourceRegistry = tenantDataSourceRegistry;
        this.masterDataSource = masterDataSource;
        this.masterHikariConfig = masterHikariConfig;
        this.passwordEncoder = passwordEncoder;
    }

    public List<TenantResumoResponse> listar() {
        return masterTenantRepository.findAll().stream().map(TenantResumoResponse::de).toList();
    }

    public TenantProvisionadoResponse provisionar(NovoTenantRequest request) {
        String slug = request.slug().toLowerCase();
        if ("superadmin".equals(slug)) {
            throw new IllegalStateException("Este slug é reservado e não pode ser usado por uma prefeitura");
        }
        masterTenantRepository.findAnyBySlug(slug).ifPresent(t -> {
            throw new IllegalStateException("Já existe uma prefeitura cadastrada com o identificador '" + slug + "'");
        });

        String dbName = "filasaude_" + slug.replace('-', '_');
        criarBancoFisico(dbName);

        Matcher matcher = JDBC_HOST_PORT.matcher(masterHikariConfig.getJdbcUrl());
        String host = "localhost";
        int port = 3306;
        if (matcher.find()) {
            host = matcher.group(1);
            port = Integer.parseInt(matcher.group(2));
        }

        TenantRecord novoTenant = new TenantRecord(
                null, slug, request.nomeMunicipio(), host, port, dbName,
                masterHikariConfig.getUsername(), masterHikariConfig.getPassword(),
                request.corPrimaria(), request.corSecundaria(), request.logoUrl(), false, true);
        masterTenantRepository.insert(novoTenant);
        log.info("Novo tenant provisionado: slug='{}', banco='{}'", slug, dbName);

        // Bug de revisão corrigido: as duas etapas abaixo (migrar o schema do
        // tenant e criar o admin inicial) não tinham nenhuma compensação se
        // falhassem -- o tenant já ficava registrado e ATIVO no master, mas sem
        // nenhum usuário capaz de logar nele, e como o slug já existia, uma nova
        // tentativa de provisionamento travava direto em "já existe uma
        // prefeitura com esse identificador", exigindo intervenção manual no
        // banco master. Agora, qualquer falha aqui desfaz o registro do master
        // (deletarPorSlug) para o operador poder tentar de novo imediatamente.
        try {
            // Aciona o registry: cria o pool de conexoes e roda o Flyway do tenant
            // (mesma logica usada no primeiro acesso normal de qualquer tenant).
            DataSource tenantDataSource = tenantDataSourceRegistry.getDataSource(slug);

            String senhaProvisoria = gerarSenhaProvisoria();
            JdbcTemplate tenantJdbc = new JdbcTemplate(tenantDataSource);
            tenantJdbc.update(
                    "INSERT INTO usuarios (nome, email, senha_hash, papel, ativo) VALUES (?, ?, ?, 'ADMIN', true)",
                    request.adminNome(), request.adminEmail(), passwordEncoder.encode(senhaProvisoria));

            return new TenantProvisionadoResponse(slug, request.nomeMunicipio(), request.adminEmail(), senhaProvisoria);
        } catch (Exception e) {
            log.error("Falha ao concluir provisionamento do tenant '{}' após registrar no master; desfazendo registro", slug, e);
            tenantDataSourceRegistry.evict(slug);
            masterTenantRepository.deletarPorSlug(slug);
            throw new IllegalStateException(
                    "Não foi possível concluir o provisionamento da prefeitura (falha ao migrar o schema ou criar o administrador inicial). "
                            + "Nada foi deixado pendente -- tente provisionar novamente.", e);
        }
    }

    public void alterarStatus(String slug, boolean ativo) {
        masterTenantRepository.findAnyBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Prefeitura não encontrada: " + slug));

        masterTenantRepository.atualizarAtivo(slug, ativo);
        // Remove do cache para que a proxima requisicao reavalie o status
        // "ativo" direto do banco master (bloqueando acesso, se desativado).
        tenantDataSourceRegistry.evict(slug);
        // Gap de revisão corrigido: nenhuma ação do superadmin gerava rastro
        // algum. Ações do superadmin não têm tenant (operam no master), então
        // não cabem no log_auditoria de um tenant específico -- ficam no log da
        // aplicação, estruturado o bastante para ser filtrado/auditado depois.
        log.info("Superadmin alterou status do tenant '{}' para ativo={}", slug, ativo);
    }

    /** Atualiza logo/cores de uma prefeitura já provisionada -- item 3.6 do levantamento de requisitos. */
    public void atualizarBranding(String slug, TenantBrandingRequest request) {
        masterTenantRepository.findAnyBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Prefeitura não encontrada: " + slug));
        masterTenantRepository.atualizarBranding(slug, request.corPrimaria(), request.corSecundaria(), request.logoUrl());
        log.info("Superadmin atualizou branding do tenant '{}' (cores)", slug);
    }

    /**
     * Upload direto de logo em PNG (item 3.6 do levantamento de requisitos), guardado como
     * binário no próprio banco master e servido depois via GET /api/public/tenant/{slug}/logo.
     * Tem prioridade sobre uma eventual URL externa cadastrada em logoUrl.
     */
    public void uploadLogo(String slug, MultipartFile arquivo) {
        masterTenantRepository.findAnyBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Prefeitura não encontrada: " + slug));

        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalStateException("Selecione um arquivo de imagem para enviar");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_LOGO) {
            throw new IllegalStateException("O logo deve ter no máximo 5MB");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_LOGO_ACEITOS.contains(contentType.toLowerCase())) {
            throw new IllegalStateException("Envie o logo em formato PNG");
        }

        try {
            masterTenantRepository.atualizarLogo(slug, arquivo.getBytes(), contentType);
            log.info("Superadmin fez upload de logo para o tenant '{}'", slug);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o arquivo enviado", e);
        }
    }

    /** Remove o logo enviado por upload -- a prefeitura volta a usar a URL externa (se houver) ou o padrão do produto. */
    public void removerLogo(String slug) {
        masterTenantRepository.findAnyBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Prefeitura não encontrada: " + slug));
        masterTenantRepository.removerLogo(slug);
        log.info("Superadmin removeu o logo do tenant '{}'", slug);
    }

    public TenantMetricasResponse metricas(String slug) {
        masterTenantRepository.findAnyBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Prefeitura não encontrada: " + slug));

        DataSource tenantDataSource = tenantDataSourceRegistry.getDataSource(slug);
        JdbcTemplate tenantJdbc = new JdbcTemplate(tenantDataSource);

        Long totalUsuarios = tenantJdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Long.class);
        Long totalPacientes = tenantJdbc.queryForObject("SELECT COUNT(*) FROM pacientes", Long.class);
        Long totalProtocolos = tenantJdbc.queryForObject("SELECT COUNT(*) FROM protocolos", Long.class);
        Long aguardando = tenantJdbc.queryForObject(
                "SELECT COUNT(*) FROM protocolos WHERE status = 'AGUARDANDO'", Long.class);

        return new TenantMetricasResponse(
                slug,
                totalUsuarios != null ? totalUsuarios : 0,
                totalPacientes != null ? totalPacientes : 0,
                totalProtocolos != null ? totalProtocolos : 0,
                aguardando != null ? aguardando : 0);
    }

    private void criarBancoFisico(String dbName) {
        try (Connection connection = masterDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            // dbName e derivado do slug ja validado por regex ([a-z0-9-]), com '-' trocado
            // por '_' -- nunca contem caracteres que permitam injecao de SQL aqui.
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4");
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Não foi possível criar o banco de dados da nova prefeitura", e);
        }
    }

    private String gerarSenhaProvisoria() {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            senha.append(caracteres.charAt(RANDOM.nextInt(caracteres.length())));
        }
        return senha.toString();
    }
}
