package br.com.filasaude.service;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.SlaConfiguracao;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.domain.enums.Papel;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.repository.UsuarioRepository;
import br.com.filasaude.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * Processa a varredura de SLA de UM tenant por vez. Extraído do
 * {@link SlaAlertaService} de propósito: o {@code @Transactional} só tem
 * efeito quando o método é chamado de fora do bean (o proxy do Spring não
 * intercepta chamadas internas/self-invocation), então o laço por tenant
 * precisa invocar este método através de um bean colaborador separado.
 */
@Service
public class SlaAlertaTenantProcessor {

    private static final Logger log = LoggerFactory.getLogger(SlaAlertaTenantProcessor.class);

    private final ProtocoloRepository protocoloRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoEmailService notificacaoEmailService;
    private final SlaConfiguracaoService slaConfiguracaoService;

    public SlaAlertaTenantProcessor(ProtocoloRepository protocoloRepository,
                                     UsuarioRepository usuarioRepository,
                                     NotificacaoEmailService notificacaoEmailService,
                                     SlaConfiguracaoService slaConfiguracaoService) {
        this.protocoloRepository = protocoloRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacaoEmailService = notificacaoEmailService;
        this.slaConfiguracaoService = slaConfiguracaoService;
    }

    /**
     * Assume que {@link TenantContext} já está setado pelo chamador (ver
     * {@link SlaAlertaService#executarVarreduraDiaria()}) para o tenant a
     * ser processado nesta chamada.
     */
    @Transactional
    public void processarTenantCorrente() {
        List<SlaConfiguracao> configuracoes = slaConfiguracaoService.carregarConfiguracoes();

        // Desde que o SLA passou a ser configurável por especialidade/
        // procedimento, não há mais uma única data-limite para filtrar em
        // SQL -- busca todos os aguardando ainda não alertados e aplica o
        // limiar resolvido por protocolo em memória (ver
        // SlaConfiguracaoService#estaAtrasado).
        List<Protocolo> atrasados = protocoloRepository
                .findByStatusAndAlertaSlaEnviadoEmIsNull(StatusProtocolo.AGUARDANDO)
                .stream()
                .filter(p -> slaConfiguracaoService.estaAtrasado(p, configuracoes))
                .toList();

        if (atrasados.isEmpty()) {
            return;
        }

        List<Usuario> destinatarios = Stream
                .concat(usuarioRepository.findByPapelOrderByNomeAsc(Papel.REGULADOR).stream(),
                        usuarioRepository.findByPapelOrderByNomeAsc(Papel.ADMIN).stream())
                .filter(Usuario::isAtivo)
                .toList();

        notificacaoEmailService.notificarProtocolosAtrasados(atrasados, destinatarios);

        LocalDateTime agora = LocalDateTime.now();
        atrasados.forEach(p -> p.setAlertaSlaEnviadoEm(agora));
        protocoloRepository.saveAll(atrasados);

        log.info("Tenant '{}': {} protocolo(s) atrasado(s) alertado(s) para {} destinatário(s)",
                TenantContext.getCurrentTenant(), atrasados.size(), destinatarios.size());
    }
}
