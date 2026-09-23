package br.com.filasaude.controller;

import br.com.filasaude.dto.superadmin.NovoTenantRequest;
import br.com.filasaude.dto.superadmin.SuperadminLoginRequest;
import br.com.filasaude.dto.superadmin.TenantBrandingRequest;
import br.com.filasaude.dto.superadmin.SuperadminLoginResponse;
import br.com.filasaude.dto.superadmin.TenantMetricasResponse;
import br.com.filasaude.dto.superadmin.TenantProvisionadoResponse;
import br.com.filasaude.dto.superadmin.TenantResumoResponse;
import br.com.filasaude.dto.superadmin.TenantStatusRequest;
import br.com.filasaude.service.SuperadminAuthService;
import br.com.filasaude.service.SuperadminTenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Painel de superadmin (item 3.6 do levantamento de requisitos, Fase 2):
 * provisionamento self-service de novas prefeituras e monitoramento básico
 * de uso. Rotas restritas ao papel SUPERADMIN (ver SecurityConfig) — não
 * pertence a nenhum tenant/prefeitura.
 */
@RestController
@RequestMapping("/api/superadmin")
public class SuperadminController {

    private final SuperadminAuthService superadminAuthService;
    private final SuperadminTenantService superadminTenantService;

    public SuperadminController(SuperadminAuthService superadminAuthService,
                                 SuperadminTenantService superadminTenantService) {
        this.superadminAuthService = superadminAuthService;
        this.superadminTenantService = superadminTenantService;
    }

    @PostMapping("/login")
    public SuperadminLoginResponse login(@Valid @RequestBody SuperadminLoginRequest request) {
        return superadminAuthService.login(request);
    }

    @GetMapping("/tenants")
    public List<TenantResumoResponse> listarTenants() {
        return superadminTenantService.listar();
    }

    @PostMapping("/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantProvisionadoResponse provisionar(@Valid @RequestBody NovoTenantRequest request) {
        return superadminTenantService.provisionar(request);
    }

    @PatchMapping("/tenants/{slug}/status")
    public void alterarStatus(@PathVariable String slug, @Valid @RequestBody TenantStatusRequest request) {
        superadminTenantService.alterarStatus(slug, request.ativo());
    }

    @PatchMapping("/tenants/{slug}/branding")
    public void atualizarBranding(@PathVariable String slug, @RequestBody TenantBrandingRequest request) {
        superadminTenantService.atualizarBranding(slug, request);
    }

    @PostMapping("/tenants/{slug}/logo")
    public void uploadLogo(@PathVariable String slug, @RequestParam("arquivo") MultipartFile arquivo) {
        superadminTenantService.uploadLogo(slug, arquivo);
    }

    @DeleteMapping("/tenants/{slug}/logo")
    public void removerLogo(@PathVariable String slug) {
        superadminTenantService.removerLogo(slug);
    }

    @GetMapping("/tenants/{slug}/metricas")
    public TenantMetricasResponse metricas(@PathVariable String slug) {
        return superadminTenantService.metricas(slug);
    }
}
