package br.com.filasaude.controller;

import br.com.filasaude.dto.publico.ProtocoloPublicoResponse;
import br.com.filasaude.service.ComprovanteService;
import br.com.filasaude.service.ProtocoloPublicoService;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.security.ConsultaPublicaRateLimiter;
import br.com.filasaude.tenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Rota pública (sem autenticação) para o cidadão acompanhar seu protocolo —
 * item 3.1 do levantamento de requisitos. Exige apenas o header X-Tenant-Id
 * (resolvido pelo frontend a partir do domínio/subdomínio da prefeitura).
 */
@RestController
@RequestMapping("/api/public/protocolo")
public class ProtocoloPublicoController {

    private final ProtocoloPublicoService protocoloPublicoService;
    private final ComprovanteService comprovanteService;
    private final ConsultaPublicaRateLimiter rateLimiter;

    public ProtocoloPublicoController(ProtocoloPublicoService protocoloPublicoService,
                                       ComprovanteService comprovanteService,
                                       ConsultaPublicaRateLimiter rateLimiter) {
        this.protocoloPublicoService = protocoloPublicoService;
        this.comprovanteService = comprovanteService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public List<ProtocoloPublicoResponse> consultar(
            @RequestParam("documento") String documento,
            @RequestParam(value = "dataNascimento", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascimento,
            HttpServletRequest request) {
        String tenant = TenantContext.getCurrentTenant();
        String origem = request.getRemoteAddr();
        rateLimiter.verificar(tenant, origem);
        try {
            List<ProtocoloPublicoResponse> resposta =
                    protocoloPublicoService.consultarPorDocumento(documento, dataNascimento);
            rateLimiter.registrarSucesso(tenant, origem);
            return resposta;
        } catch (ResourceNotFoundException e) {
            rateLimiter.registrarFalha(tenant, origem);
            throw e;
        }
    }

    /**
     * Emissão de comprovante em PDF (itens 3.1/3.5): exige o mesmo CPF/CNS
     * usado na consulta, para que o comprovante só possa ser baixado por
     * quem já provou conhecer o documento do paciente.
     */
    @GetMapping(value = "/{numeroProtocolo}/comprovante", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> comprovante(@PathVariable String numeroProtocolo,
                                               @RequestParam("documento") String documento,
                                               @RequestParam(value = "dataNascimento", required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascimento,
                                               HttpServletRequest request) {
        String tenant = TenantContext.getCurrentTenant();
        String origem = request.getRemoteAddr();
        rateLimiter.verificar(tenant, origem);
        byte[] pdf;
        try {
            pdf = comprovanteService.gerar(numeroProtocolo, documento, dataNascimento);
        } catch (ResourceNotFoundException e) {
            rateLimiter.registrarFalha(tenant, origem);
            throw e;
        }
        String nomeArquivo = "comprovante-" + numeroProtocolo + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
