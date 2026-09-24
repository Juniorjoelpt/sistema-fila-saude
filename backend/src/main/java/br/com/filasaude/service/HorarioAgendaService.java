package br.com.filasaude.service;

import br.com.filasaude.domain.HorarioAgenda;
import br.com.filasaude.domain.UnidadeSaude;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.dto.agenda.HorarioAgendaCreateRequest;
import br.com.filasaude.dto.agenda.HorarioAgendaLoteRequest;
import br.com.filasaude.dto.agenda.HorarioAgendaResponse;
import br.com.filasaude.exception.ResourceNotFoundException;
import br.com.filasaude.repository.HorarioAgendaRepository;
import br.com.filasaude.repository.ProtocoloRepository;
import br.com.filasaude.repository.UnidadeSaudeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Gestão da agenda de horários reais (unidade + especialidade + data + hora,
 * com capacidade de vagas): cadastro individual ou em lote (recorrente por
 * dia da semana), listagem com vagas ocupadas/disponíveis calculadas em
 * tempo real, e remoção de horários ainda não utilizados.
 *
 * Assim como a Cota mensal, a quantidade ocupada não é armazenada aqui --
 * é sempre contada a partir dos protocolos vinculados (status != CANCELADO),
 * para nunca ficar dessincronizada da fila real.
 */
@Service
@Transactional
public class HorarioAgendaService {

    private final HorarioAgendaRepository horarioAgendaRepository;
    private final UnidadeSaudeRepository unidadeSaudeRepository;
    private final ProtocoloRepository protocoloRepository;
    private final AuditoriaService auditoriaService;

    public HorarioAgendaService(HorarioAgendaRepository horarioAgendaRepository,
                                 UnidadeSaudeRepository unidadeSaudeRepository,
                                 ProtocoloRepository protocoloRepository,
                                 AuditoriaService auditoriaService) {
        this.horarioAgendaRepository = horarioAgendaRepository;
        this.unidadeSaudeRepository = unidadeSaudeRepository;
        this.protocoloRepository = protocoloRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<HorarioAgendaResponse> listar(Long unidadeSaudeId, String especialidade,
                                               LocalDate dataInicio, LocalDate dataFim) {
        if (unidadeSaudeId == null) {
            throw new IllegalStateException("Informe a unidade de saúde para listar a agenda.");
        }
        LocalDate inicio = dataInicio != null ? dataInicio : LocalDate.now();
        LocalDate fim = dataFim != null ? dataFim : inicio.plusDays(30);

        List<HorarioAgenda> horarios = (especialidade != null && !especialidade.isBlank())
                ? horarioAgendaRepository.findByUnidadeSaudeIdAndEspecialidadeAndDataBetweenOrderByDataAscHoraInicioAsc(
                        unidadeSaudeId, especialidade, inicio, fim)
                : horarioAgendaRepository.findByUnidadeSaudeIdAndDataBetweenOrderByDataAscHoraInicioAsc(
                        unidadeSaudeId, inicio, fim);

        return horarios.stream().map(this::toResponse).toList();
    }

    public HorarioAgendaResponse criar(HorarioAgendaCreateRequest request) {
        UnidadeSaude unidade = buscarUnidade(request.unidadeSaudeId());

        if (request.data().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Não é possível cadastrar um horário em uma data passada.");
        }

        horarioAgendaRepository.findByUnidadeSaudeIdAndEspecialidadeAndDataAndHoraInicio(
                        request.unidadeSaudeId(), request.especialidade(), request.data(), request.horaInicio())
                .ifPresent(h -> {
                    throw new IllegalStateException(
                            "Já existe um horário cadastrado para esta unidade, especialidade, data e hora.");
                });

        HorarioAgenda horario = HorarioAgenda.builder()
                .unidadeSaude(unidade)
                .especialidade(request.especialidade())
                .data(request.data())
                .horaInicio(request.horaInicio())
                .capacidadeTotal(request.capacidadeTotal())
                .build();

        HorarioAgenda salvo = horarioAgendaRepository.save(horario);
        auditoriaService.registrar("CRIAR_HORARIO_AGENDA", "HorarioAgenda", salvo.getId(),
                "Horário criado: " + unidade.getNome() + " / " + request.especialidade()
                        + " em " + request.data() + " " + request.horaInicio()
                        + " (" + request.capacidadeTotal() + " vaga(s))");

        return toResponse(salvo);
    }

    /**
     * Gera horários recorrentes: um para cada combinação de dia-da-semana
     * selecionado dentro do período, do horário inicial ao final, em
     * intervalos fixos. Horários que já existam (mesma unidade/especialidade
     * /data/hora) são pulados silenciosamente, então a operação pode ser
     * repetida com segurança para completar um período sem duplicar nada.
     */
    public int gerarLote(HorarioAgendaLoteRequest request) {
        UnidadeSaude unidade = buscarUnidade(request.unidadeSaudeId());

        if (request.dataFim().isBefore(request.dataInicio())) {
            throw new IllegalStateException("A data final não pode ser anterior à data inicial.");
        }
        if (!request.horaFim().isAfter(request.horaInicio())) {
            throw new IllegalStateException("A hora final deve ser depois da hora inicial.");
        }

        int criados = 0;
        for (LocalDate data = request.dataInicio(); !data.isAfter(request.dataFim()); data = data.plusDays(1)) {
            if (!request.diasSemana().contains(data.getDayOfWeek())) {
                continue;
            }
            for (LocalTime hora = request.horaInicio(); hora.isBefore(request.horaFim());
                 hora = hora.plusMinutes(request.intervaloMinutos())) {

                boolean jaExiste = horarioAgendaRepository.findByUnidadeSaudeIdAndEspecialidadeAndDataAndHoraInicio(
                        request.unidadeSaudeId(), request.especialidade(), data, hora).isPresent();
                if (jaExiste) {
                    continue;
                }

                horarioAgendaRepository.save(HorarioAgenda.builder()
                        .unidadeSaude(unidade)
                        .especialidade(request.especialidade())
                        .data(data)
                        .horaInicio(hora)
                        .capacidadeTotal(request.capacidadePorHorario())
                        .build());
                criados++;
            }
        }

        auditoriaService.registrar("GERAR_LOTE_HORARIOS_AGENDA", "HorarioAgenda", null,
                criados + " horário(s) gerado(s) para " + unidade.getNome() + " / " + request.especialidade()
                        + " entre " + request.dataInicio() + " e " + request.dataFim());

        return criados;
    }

    public void remover(Long id) {
        HorarioAgenda horario = horarioAgendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horário não encontrado: " + id));

        long ocupadas = protocoloRepository.countByHorarioAgendadoIdAndStatusNot(id, StatusProtocolo.CANCELADO);
        if (ocupadas > 0) {
            throw new IllegalStateException(
                    "Não é possível remover um horário com protocolo(s) agendado(s). Cancele o(s) agendamento(s) primeiro.");
        }

        horarioAgendaRepository.delete(horario);
        auditoriaService.registrar("REMOVER_HORARIO_AGENDA", "HorarioAgenda", id,
                "Horário removido: " + horario.getUnidadeSaude().getNome() + " / " + horario.getEspecialidade()
                        + " em " + horario.getData() + " " + horario.getHoraInicio());
    }

    private UnidadeSaude buscarUnidade(Long unidadeSaudeId) {
        return unidadeSaudeRepository.findById(unidadeSaudeId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade de saúde não encontrada: " + unidadeSaudeId));
    }

    private HorarioAgendaResponse toResponse(HorarioAgenda h) {
        long ocupadas = protocoloRepository.countByHorarioAgendadoIdAndStatusNot(h.getId(), StatusProtocolo.CANCELADO);
        return HorarioAgendaResponse.de(h, ocupadas);
    }
}
