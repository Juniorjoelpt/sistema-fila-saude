package br.com.filasaude.service;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.dto.publico.EtapaPublicaResponse;
import br.com.filasaude.dto.publico.ProtocoloPublicoResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.ProtocoloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Módulo do cidadão (item 3.1): consulta pública de protocolo por CPF ou CNS,
 * sem necessidade de login. Retorna o histórico completo de protocolos do
 * paciente (mais recente primeiro), cada um com sua timeline de etapas.
 */
@Service
@Transactional(readOnly = true)
public class ProtocoloPublicoService {

    private final ProtocoloRepository protocoloRepository;
    private final FilaPriorizacaoService filaPriorizacaoService;

    public ProtocoloPublicoService(ProtocoloRepository protocoloRepository,
                                    FilaPriorizacaoService filaPriorizacaoService) {
        this.protocoloRepository = protocoloRepository;
        this.filaPriorizacaoService = filaPriorizacaoService;
    }

    public List<ProtocoloPublicoResponse> consultarPorDocumento(String documentoBruto) {
        String documento = apenasDigitos(documentoBruto);
        if (documento.isBlank()) {
            throw new IllegalArgumentException("Informe um CPF ou CNS válido");
        }

        List<Protocolo> protocolos = protocoloRepository.findByDocumentoPaciente(documento);
        if (protocolos.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Nenhum protocolo encontrado para o CPF/CNS informado");
        }

        return protocolos.stream().map(this::toResponse).toList();
    }

    private ProtocoloPublicoResponse toResponse(Protocolo protocolo) {
        Integer posicao = protocolo.getStatus().name().equals("AGUARDANDO")
                ? filaPriorizacaoService.posicaoNaFila(protocolo.getId()).orElse(null)
                : null;

        List<EtapaPublicaResponse> etapas = protocolo.getEtapas().stream()
                .map(EtapaPublicaResponse::de)
                .toList();

        return new ProtocoloPublicoResponse(
                protocolo.getNumeroProtocolo(),
                protocolo.getPaciente().getNome(),
                protocolo.getProcedimento().getNome(),
                protocolo.getUnidadeSaude() != null ? protocolo.getUnidadeSaude().getNome() : null,
                protocolo.getStatus().name(),
                posicao,
                protocolo.getDataInclusao(),
                protocolo.getDataPrevista(),
                protocolo.getHorarioAgendado() != null ? protocolo.getHorarioAgendado().getHoraInicio() : null,
                etapas
        );
    }

    private String apenasDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
