package br.com.filasaude.security;

import br.com.filasaude.tenancy.TenantDataSourceRegistry;
import br.com.filasaude.tenancy.TenantResolverFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Registrado manualmente aqui (não como @Component/@Order genérico) para
    // garantir que rode DENTRO da cadeia do Spring Security, antes do
    // JwtAuthenticationFilter -- ver o motivo detalhado no Javadoc de
    // TenantResolverFilter.
    @Bean
    public TenantResolverFilter tenantResolverFilter(TenantDataSourceRegistry registry) {
        return new TenantResolverFilter(registry);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TenantResolverFilter tenantResolverFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rotas publicas: consulta de protocolo pelo cidadao e autenticacao
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/superadmin/login").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Painel de superadmin (item 3.6, Fase 2): fornecedor do SaaS, fora de
                        // qualquer tenant
                        .requestMatchers("/api/superadmin/**").hasRole(JwtService.ROLE_SUPERADMIN)
                        // Log de auditoria geral do sistema (item 3.5): restrito a Admin
                        .requestMatchers("/api/admin/auditoria/**").hasRole("ADMIN")
                        // Configuracao das integracoes obrigatorias com o Ministerio da Saude
                        // (e-SUS, SISREG, CNES): credenciais sensiveis, restrito a Admin
                        .requestMatchers("/api/admin/integracoes/**").hasRole("ADMIN")
                        // Painel administrativo (ACS, Regulador, Admin)
                        .requestMatchers("/api/admin/**").hasAnyRole("ACS", "REGULADOR", "ADMIN")
                        // Gestao de fila: acoes de regulacao (mudar status/prioridade, marcar
                        // etapa, distribuir vagas em lote, exportar planilha) sao privativas de
                        // Regulador/Admin -- o ACS (item 2 do levantamento de requisitos) so
                        // cadastra/acompanha os proprios pacientes, sem poder de regulacao
                        .requestMatchers(HttpMethod.GET, "/api/fila/export").hasAnyRole("REGULADOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/fila/distribuir-vagas").hasAnyRole("REGULADOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/fila/*/agendar-horario").hasAnyRole("REGULADOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/fila/*/status", "/api/fila/*/prioridade", "/api/fila/*/etapas/*")
                            .hasAnyRole("REGULADOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/fila/**").hasAnyRole("ACS", "REGULADOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/fila").hasAnyRole("ACS", "REGULADOR", "ADMIN")
                        // Gap de revisão corrigido: um paciente cadastrado por Regulador/Admin
                        // sem escolher ACS responsável ficava "órfão" para sempre (invisível a
                        // qualquer ACS, sem forma de corrigir). Reatribuir o ACS responsável é
                        // privativo de Regulador/Admin, pelo mesmo motivo que o cadastro do ACS
                        // nunca aceita um acsResponsavelId vindo do cliente (PacienteService).
                        .requestMatchers(HttpMethod.PATCH, "/api/pacientes/*/acs-responsavel").hasAnyRole("REGULADOR", "ADMIN")
                        .requestMatchers("/api/pacientes/**").hasAnyRole("ACS", "REGULADOR", "ADMIN")
                        // Leitura de procedimentos/unidades liberada tambem ao ACS (precisa
                        // delas para abrir um protocolo do proprio paciente); escrita e as
                        // demais gestoes (cotas, importacao em lote) seguem privativas de
                        // Regulador/Admin
                        .requestMatchers(HttpMethod.GET, "/api/procedimentos/**", "/api/unidades/**")
                            .hasAnyRole("ACS", "REGULADOR", "ADMIN")
                        .requestMatchers("/api/procedimentos/**", "/api/unidades/**", "/api/cotas/**", "/api/importacao/**",
                                "/api/horarios-agenda/**")
                            .hasAnyRole("REGULADOR", "ADMIN")
                        // Gestao de equipe (item 3.4): leitura liberada (ex.: filtro "ACS
                        // responsavel" na fila), escrita restrita a Admin
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**").hasAnyRole("ACS", "REGULADOR", "ADMIN")
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // jwtAuthenticationFilter precisa ser registrado primeiro para que sua
                // posição na cadeia seja conhecida -- só então dá para inserir o
                // tenantResolverFilter imediatamente antes dele (ele precisa rodar
                // ANTES, pois o jwtAuthenticationFilter valida a claim "tenant" do
                // token contra o TenantContext já resolvido).
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(tenantResolverFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
