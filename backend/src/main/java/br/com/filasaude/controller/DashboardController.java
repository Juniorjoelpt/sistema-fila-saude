package br.com.filasaude.controller;

import br.com.filasaude.dto.dashboard.DashboardResponse;
import br.com.filasaude.dto.dashboard.PrevisaoSlaResponse;
import br.com.filasaude.service.DashboardService;
import br.com.filasaude.service.PrevisaoSlaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final PrevisaoSlaService previsaoSlaService;

    public DashboardController(DashboardService dashboardService, PrevisaoSlaService previsaoSlaService) {
        this.dashboardService = dashboardService;
        this.previsaoSlaService = previsaoSlaService;
    }

    /** Dashboard preditivo de SLA (roadmap #5): quem vai estourar o prazo no ritmo atual de atendimento. */
    @GetMapping("/previsao-sla")
    public PrevisaoSlaResponse previsaoSla() {
        return previsaoSlaService.gerar();
    }

    @GetMapping
    public DashboardResponse dashboard() {
        return dashboardService.gerar();
    }
}
