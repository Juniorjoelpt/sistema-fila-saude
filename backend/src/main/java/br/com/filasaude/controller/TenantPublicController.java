package br.com.filasaude.controller;

import br.com.filasaude.dto.superadmin.TenantBrandingResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.tenancy.MasterTenantRepository;
import br.com.filasaude.tenancy.TenantContext;
import br.com.filasaude.tenancy.TenantLogo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Identidade visual (logo e cores) do tenant corrente -- item 3.6 do
 * levantamento de requisitos. Exposta sem autenticação (rota GET /api/public/**
 * já é permitAll no SecurityConfig) para que a tela pública de consulta de
 * protocolo do cidadão e a tela de login apliquem o logo/cores da Secretaria
 * antes mesmo de qualquer login. Não depende do banco do tenant -- só do
 * cadastro no banco master -- então funciona mesmo que o schema do tenant
 * ainda não tenha sido inicializado.
 */
@RestController
@RequestMapping("/api/public/tenant")
public class TenantPublicController {

    private final MasterTenantRepository masterTenantRepository;

    public TenantPublicController(MasterTenantRepository masterTenantRepository) {
        this.masterTenantRepository = masterTenantRepository;
    }

    @GetMapping("/branding")
    public TenantBrandingResponse branding() {
        String slug = TenantContext.getCurrentTenant();
        if (slug == null) {
            return TenantBrandingResponse.padrao();
        }
        return masterTenantRepository.findBySlug(slug)
                .map(TenantBrandingResponse::de)
                .orElse(TenantBrandingResponse.padrao());
    }

    /** Serve o binário do logo enviado por upload pelo superadmin (ver SuperadminController). */
    @GetMapping("/{slug}/logo")
    public ResponseEntity<byte[]> logo(@PathVariable String slug) {
        TenantLogo logo = masterTenantRepository.buscarLogo(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Logo não encontrado para esta prefeitura"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .header("Cache-Control", "public, max-age=3600")
                .body(logo.dados());
    }
}
