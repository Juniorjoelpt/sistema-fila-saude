package br.com.filasaude.repository;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.StatusProtocolo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProtocoloRepository extends JpaRepository<Protocolo, Long>, JpaSpecificationExecutor<Protocolo> {

    Optional<Protocolo> findByNumeroProtocolo(String numeroProtocolo);

    @Query("""
           SELECT p FROM Protocolo p
           JOIN FETCH p.paciente pac
           WHERE (pac.cpf = :documento OR pac.cns = :documento)
           ORDER BY p.criadoEm DESC
           """)
    List<Protocolo> findByDocumentoPaciente(@Param("documento") String documento);

    long countByStatus(StatusProtocolo status);

    /**
     * Todos os protocolos aguardando atendimento. A ordenacao pelo motor de
     * priorizacao (categoria + FIFO por data) e feita em memoria pelo
     * {@code FilaPriorizacaoService}, mais simples e legivel do que replicar
     * a logica de peso das categorias em SQL.
     */
    List<Protocolo> findByStatus(StatusProtocolo status);

    /**
     * Protocolos de uma unidade+especialidade num mes de referencia -- usado
     * para calcular o percentual de ocupacao de uma cota (item 3.3). O
     * filtro de status CANCELADO fica no service, para nao acoplar a query a
     * enum literal em JPQL.
     */
    List<Protocolo> findByUnidadeSaudeIdAndProcedimento_EspecialidadeAndDataInclusaoBetween(
            Long unidadeSaudeId, String especialidade, java.time.LocalDate inicio, java.time.LocalDate fim);

    /**
     * Protocolos ainda aguardando atendimento cuja data de inclusão já ultrapassou o
     * limite de dias considerado "Atrasado" (mesmo critério do badge de prazo da fila,
     * ver {@code nivelPrazo} no frontend) e que ainda não geraram alerta de SLA --
     * usado pela varredura diária do {@code SlaAlertaService}.
     */
    List<Protocolo> findByStatusAndDataInclusaoLessThanEqualAndAlertaSlaEnviadoEmIsNull(
            StatusProtocolo status, java.time.LocalDate dataLimite);

    /**
     * Quantos protocolos ainda "valem" (status != CANCELADO) estão ocupando
     * um determinado horário de agenda -- usado para checar vaga disponível
     * antes de confirmar um agendamento e para exibir o preenchimento de
     * cada horário na tela de gestão de agenda (ver HorarioAgendaService).
     */
    long countByHorarioAgendadoIdAndStatusNot(Long horarioAgendadoId, StatusProtocolo status);

    /**
     * Protocolos com horário real marcado (ver HorarioAgenda) para uma data
     * específica, ainda no status AGENDADO e sem lembrete enviado -- usado
     * pela varredura diária do {@code LembreteAgendamentoService}. Traz já
     * carregados (JOIN FETCH) tudo que o e-mail de lembrete precisa exibir
     * (paciente, procedimento, horário e unidade), para que a leitura desses
     * dados dentro do método @Async de envio (rodando em outra thread, sem
     * sessão do Hibernate) seja segura -- os proxies já chegam inicializados.
     */
    @Query("""
           SELECT p FROM Protocolo p
           JOIN FETCH p.paciente pac
           JOIN FETCH p.procedimento proc
           JOIN FETCH p.horarioAgendado h
           JOIN FETCH h.unidadeSaude u
           WHERE p.status = :status
             AND h.data = :data
             AND p.lembreteEnviadoEm IS NULL
           """)
    List<Protocolo> findParaLembreteAgendamento(@Param("status") StatusProtocolo status,
                                                 @Param("data") LocalDate data);

    /**
     * Localiza o protocolo pelo token de confirmação de presença enviado no
     * lembrete por e-mail -- ver {@code ProtocoloConfirmacaoController}.
     * Também com JOIN FETCH, pelo mesmo motivo: a tela pública de confirmação
     * exibe unidade/data/hora sem exigir login.
     */
    @Query("""
           SELECT p FROM Protocolo p
           JOIN FETCH p.paciente pac
           JOIN FETCH p.procedimento proc
           LEFT JOIN FETCH p.horarioAgendado h
           LEFT JOIN FETCH h.unidadeSaude u
           WHERE p.confirmacaoToken = :token
           """)
    Optional<Protocolo> findByConfirmacaoToken(@Param("token") String token);
}
