package br.com.filasaude.service;

import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.domain.enums.CategoriaPrioridade;
import br.com.filasaude.domain.enums.Papel;
import br.com.filasaude.dto.protocolo.ProtocoloCreateRequest;
import br.com.filasaude.dto.protocolo.ProtocoloResponse;
import br.com.filasaude.repository.HistoricoPrioridadeRepository;
import br.com.filasaude.repository.HistoricoStatusRepository;
import br.com.filasaude.repository.HorarioAgendaRepository;
import br.com.filasaude.repository.PacienteRepository;
import br.com.filasaude.repository.ProcedimentoRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.repository.UnidadeSaudeRepository;
import br.com.filasaude.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;

import static br.com.filasaude.support.Fixtures.paciente;
import static br.com.filasaude.support.Fixtures.procedimento;
import static br.com.filasaude.support.Fixtures.protocolo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Urgência/Judicial pedidas por ACS nascem "pendentes de confirmação do regulador":
 * entram como Normal e só viram a categoria pedida quando o regulador confirma.
 */
@ExtendWith(MockitoExtension.class)
class ProtocoloServicePrioridadeSolicitadaTest {

    @Mock private ProtocoloRepository protocoloRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private ProcedimentoRepository procedimentoRepository;
    @Mock private UnidadeSaudeRepository unidadeSaudeRepository;
    @Mock private HistoricoStatusRepository historicoStatusRepository;
    @Mock private HistoricoPrioridadeRepository historicoPrioridadeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HorarioAgendaRepository horarioAgendaRepository;
    @Mock private EtapasPadraoFactory etapasPadraoFactory;
    @Mock private FilaPriorizacaoService filaPriorizacaoService;
    @Mock private NotificacaoEmailService notificacaoEmailService;
    @Mock private AuditoriaService auditoriaService;
    @Mock private CotaService cotaService;
    private final ClassificacaoRiscoService classificacaoRiscoService = new ClassificacaoRiscoService();

    private ProtocoloService service() {
        return new ProtocoloService(protocoloRepository, pacienteRepository, procedimentoRepository,
                unidadeSaudeRepository, historicoStatusRepository, historicoPrioridadeRepository,
                usuarioRepository, horarioAgendaRepository, etapasPadraoFactory, filaPriorizacaoService,
                notificacaoEmailService, auditoriaService, cotaService, classificacaoRiscoService);
    }

    private final Paciente paciente = paciente(1L, "Paciente Teste");
    private final Procedimento procedimento = procedimento(1L, "Consulta Cardiologia", "Cardiologia");

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private Usuario logarComo(Papel papel, long id) {
        Usuario u = Usuario.builder().id(id).nome("Usuario " + papel).email(papel.name().toLowerCase() + "@teste.com")
                .papel(papel).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u.getEmail(), null));
        when(usuarioRepository.findByEmailIgnoreCase(u.getEmail())).thenReturn(Optional.of(u));
        return u;
    }

    private void prepararCriacao() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(procedimentoRepository.findById(1L)).thenReturn(Optional.of(procedimento));
        when(protocoloRepository.save(any(Protocolo.class))).thenAnswer(inv -> {
            Protocolo p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(50L);
            }
            return p;
        });
    }

    private ProtocoloCreateRequest pedido(CategoriaPrioridade categoria) {
        return new ProtocoloCreateRequest(1L, 1L, null, categoria,
                categoria == CategoriaPrioridade.JUDICIAL ? "0001234-56.2026.8.18.0001" : null, LocalDate.now());
    }

    @Test
    void urgenciaPedidaPorAcsNasceNormalEPendente() {
        Usuario acs = logarComo(Papel.ACS, 7L);
        paciente.setAcsResponsavel(acs);
        prepararCriacao();

        ProtocoloResponse r = service().criar(pedido(CategoriaPrioridade.URGENCIA));

        assertThat(r.categoriaPrioridade()).isEqualTo("NORMAL");
        assertThat(r.categoriaSolicitada()).isEqualTo("URGENCIA");
    }

    @Test
    void judicialPedidoPorAcsNasceNormalEPendente() {
        Usuario acs = logarComo(Papel.ACS, 7L);
        paciente.setAcsResponsavel(acs);
        prepararCriacao();

        ProtocoloResponse r = service().criar(pedido(CategoriaPrioridade.JUDICIAL));

        assertThat(r.categoriaPrioridade()).isEqualTo("NORMAL");
        assertThat(r.categoriaSolicitada()).isEqualTo("JUDICIAL");
    }

    @Test
    void urgenciaPedidaPorReguladorEntraDiretoSemPendencia() {
        logarComo(Papel.REGULADOR, 8L);
        prepararCriacao();

        ProtocoloResponse r = service().criar(pedido(CategoriaPrioridade.URGENCIA));

        assertThat(r.categoriaPrioridade()).isEqualTo("URGENCIA");
        assertThat(r.categoriaSolicitada()).isNull();
    }

    @Test
    void reguladorConfirmaESolicitacaoViraCategoriaOficial() {
        logarComo(Papel.REGULADOR, 8L);
        Protocolo p = protocolo(10L, paciente, procedimento);
        p.setCategoriaPrioridade(CategoriaPrioridade.NORMAL);
        p.setCategoriaSolicitada(CategoriaPrioridade.URGENCIA);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(p));

        ProtocoloResponse r = service().decidirPrioridadeSolicitada(10L, true, null);

        assertThat(r.categoriaPrioridade()).isEqualTo("URGENCIA");
        assertThat(r.categoriaSolicitada()).isNull();
        verify(historicoPrioridadeRepository).save(any());
    }

    @Test
    void reguladorRecusaExigeMotivoEMantemNormal() {
        logarComo(Papel.REGULADOR, 8L);
        Protocolo p = protocolo(10L, paciente, procedimento);
        p.setCategoriaPrioridade(CategoriaPrioridade.NORMAL);
        p.setCategoriaSolicitada(CategoriaPrioridade.URGENCIA);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service().decidirPrioridadeSolicitada(10L, false, " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("motivo");
        verify(protocoloRepository, never()).save(any());

        ProtocoloResponse r = service().decidirPrioridadeSolicitada(10L, false, "Sem critério clínico de urgência");

        assertThat(r.categoriaPrioridade()).isEqualTo("NORMAL");
        assertThat(r.categoriaSolicitada()).isNull();
    }

    @Test
    void decidirSemSolicitacaoPendenteFalha() {
        Protocolo p = protocolo(10L, paciente, procedimento);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service().decidirPrioridadeSolicitada(10L, true, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pendente");
    }
}
