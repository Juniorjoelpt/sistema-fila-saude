package br.com.filasaude.controller;

import br.com.filasaude.dto.transparencia.TransparenciaResponse;
import br.com.filasaude.service.TransparenciaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Transparência ativa (LAI), sem autenticação (GET /api/public/** é permitAll).
 * Só indicadores agregados. Responde 404 enquanto o secretário não publicar.
 */
@RestController
@RequestMapping("/api/public/transparencia")
public class TransparenciaPublicaController {

    private final TransparenciaService transparenciaService;

    public TransparenciaPublicaController(TransparenciaService transparenciaService) {
        this.transparenciaService = transparenciaService;
    }

    @GetMapping
    public TransparenciaResponse indicadores() {
        return transparenciaService.gerarPublico();
    }

    /** Dados abertos: {@code formato=csv} (padrão) ou {@code formato=json}, como download. */
    @GetMapping("/export")
    public ResponseEntity<?> exportar(@RequestParam(value = "formato", defaultValue = "csv") String formato) {
        TransparenciaResponse dados = transparenciaService.gerarPublico();
        String data = LocalDate.now().toString();
        if ("json".equalsIgnoreCase(formato)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transparencia-fila-" + data + ".json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dados);
        }
        if (!"csv".equalsIgnoreCase(formato)) {
            throw new IllegalArgumentException("Formato inválido: use csv ou json.");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transparencia-fila-" + data + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(transparenciaService.paraCsv(dados).getBytes(StandardCharsets.UTF_8));
    }
}
