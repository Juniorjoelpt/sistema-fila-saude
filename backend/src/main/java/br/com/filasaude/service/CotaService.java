package br.com.filasaude.service;

import br.com.filasaude.domain.Cota;
import br.com.filasaude.domain.CotaAjuste;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.UnidadeSaude;
import br.com.filasaude.domain.Usuario;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.cota.CotaAjusteRequest;
import br.com.filasaude.dto.cota.CotaAjusteResponse;
import br.com.filasaude.dto.cota.CotaCreateRequest;
import br.com.filasaude.dto.cota.CotaResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.CotaAjusteRepository;
import br.com.filasaude.repository.CotaRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.repository.UnidadeSaudeRepository;
import br.com.filasaude.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Gestão de cotas por unidade de saúde e especialidade (item 3.3 do
 * levantamento de requisitos, Fase 2): cadastro/edição de cotas, ajuste em
 * tempo real com registro de quem ajustou e quando, e indicador de
 * percentual de vagas preenchidas.
 *
 * A quantidade utilizada não é um contador manual: é calculada a partir dos
 * protocolos já vinculados àquela unidade/especialidade/mês (status
 * diferente de CANCELADO), para nunca ficar dessincronizada da fila real.
 */
@Service
@Transactional
public class CotaService {

    private final CotaRepository cotaRepository;
    private final CotaAjusteRepository cotaAjusteRepository;
    private final UnidadeSaudeRepository unidadeSaudeRepository;
    private final ProtocoloRepository protocoloRepository;
    private final UsuarioRepository usuarioRepository;

    public CotaService(CotaRepository cotaRepository,
                        CotaAjusteRepository cotaAjusteRepository,
                        UnidadeSaudeRepository unidadeSaudeRepository,
                        ProtocoloRepository protocoloRepository,
                        UsuarioRepository usuarioRepository) {
        this.cotaRepository = cotaRepository;
        this.cotaAjusteRepository = cotaAjusteRepository;
        this.unidadeSaudeRepository = unidadeSaudeRepository;
        this.protocoloRepository = protocoloRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<CotaResponse> listar(Long unidadeSaudeId, YearMonth mesReferencia) {
        YearMonth mes = mesReferencia != null ? mesReferencia : YearMonth.now();
        LocalDate inicioMes = mes.atDay(1);

        List<Cota> cotas = unidadeSaudeId != null
                ? cotaRepository.findByUnidadeSaudeIdAndMesReferenciaOrderByEspecialidadeAsc(unidadeSaudeId, inicioMes)
                : cotaRepository.findByMesReferenciaOrderByUnidadeSaude_NomeAscEspecialidadeAsc(inicioMes);

        return cotas.stream().map(this::toResponse).toList();
    }

    public CotaResponse criar(CotaCreateRequest request) {
        UnidadeSaude unidade = unidadeSaudeRepository.findById(request.unidadeSaudeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade de saúde não encontrada: " + request.unidadeSaudeId()));

        LocalDate mesNormalizado = request.mesReferencia().atDay(1);
        cotaRepository.findByUnidadeSaudeIdAndEspecialidadeAndMesReferencia(
                request.unidadeSaudeId(), request.especialidade(), mesNormalizado)
                .ifPresent(c -> {
                    throw new IllegalStateException(
                            "Já existe uma cota cadastrada para esta unidade, especialidade e mês");
                });

        Cota cota = Cota.builder()
                .unidadeSaude(unidade)
                .especialidade(request.especialidade())
                .mesReferencia(mesNormalizado)
                .quantidadeTotal(request.quantidadeTotal())
                .build();

        return toResponse(cotaRepository.save(cota));
    }

    public CotaResponse ajustar(Long id, CotaAjusteRequest request) {
        Cota cota = cotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cota não encontrada: " + id));

        int quantidadeAnterior = cota.getQuantidadeTotal();
        cota.setQuantidadeTotal(request.quantidadeTotal());
        cota.setAtualizadoEm(java.time.LocalDateTime.now());
        cotaRepository.save(cota);

        cotaAjusteRepository.save(CotaAjuste.builder()
                .cota(cota)
                .quantidadeAnterior(quantidadeAnterior)
                .quantidadeNova(request.quantidadeTotal())
                .usuario(usuarioLogado().orElse(null))
                .motivo(request.motivo())
                .build());

        return toResponse(cota);
    }

    @Transactional(readOnly = true)
    public List<CotaAjusteResponse> historico(Long cotaId) {
        if (!cotaRepository.existsById(cotaId)) {
            throw new ResourceNotFoundException("Cota não encontrada: " + cotaId);
        }
        return cotaAjusteRepository.findByCotaIdOrderByCriadoEmDesc(cotaId).stream()
                .map(CotaAjusteResponse::de)
                .toList();
    }

    private CotaResponse toResponse(Cota cota) {
        LocalDate inicio = cota.getMesReferencia();
        LocalDate fim = YearMonth.from(inicio).atEndOfMonth();

        List<Protocolo> protocolosDoMes = protocoloRepository
                .findByUnidadeSaudeIdAndProcedimento_EspecialidadeAndDataInclusaoBetween(
                        cota.getUnidadeSaude().getId(), cota.getEspecialidade(), inicio, fim);

        long utilizada = protocolosDoMes.stream()
                .filter(p -> p.getStatus() != StatusProtocolo.CANCELADO)
                .count();

        return CotaResponse.de(cota, utilizada);
    }

    private Optional<Usuario> usuarioLogado() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(a -> a.getName())
                .flatMap(usuarioRepository::findByEmailIgnoreCase);
    }
}
