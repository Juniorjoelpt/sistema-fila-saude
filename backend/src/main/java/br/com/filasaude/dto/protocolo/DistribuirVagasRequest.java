package br.com.filasaude.dto.protocolo;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Ação "Distribuir Vagas" (item 3.2): agenda em lote uma lista de
 * protocolos para uma data solta (sem horário/vaga real controlada -- ver
 * {@link br.com.filasaude.service.ProtocoloService#agendarHorario} para o
 * fluxo com horário real).
 *
 * Bug alto de revisão corrigido: {@code dataPrevista} não tinha
 * {@code @NotNull}, então um request sem esse campo (ou com {@code null}
 * explícito) mudava o status do protocolo para AGENDADO sem nenhuma data
 * associada -- um "agendamento" sem data. {@code @FutureOrPresent} impede
 * também agendar para uma data já passada, consistente com a mesma regra
 * aplicada ao cadastro de horário de agenda.
 */
public record DistribuirVagasRequest(
        @NotEmpty List<Long> protocoloIds,
        @NotNull @FutureOrPresent LocalDate dataPrevista
) {
}
