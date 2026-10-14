package br.com.filasaude.repository;

import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.PresencaConfirmacao;
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

    /** Entradas na fila por procedimento desde uma data (linhas [procedimentoId, quantidade]) -- ver PrevisaoSlaService. */
    @Query("SELECT p.procedimento.id, COUNT(p) FROM Protocolo p WHERE p.dataInclusao >= :desde GROUP BY p.procedimento.id")
    List<Object[]> contarEntradasDesde(@Param("desde") LocalDate desde);

    /**
     * Atendimentos concluídos por mês (transparência ativa). Para protocolo encerrado,
     * {@code atualizadoEm} é a data do encerramento (ver Protocolo#diasEmEspera).
     * Retorna linhas [ano, mês, quantidade].
     */
    @Query("""
           SELECT YEAR(p.atualizadoEm), MONTH(p.atualizadoEm), COUNT(p) FROM Protocolo p
           WHERE p.status = br.com.filasaude.domain.enums.StatusProtocolo.CONCLUIDO AND p.atualizadoEm >= :desde
           GROUP BY YEAR(p.atualizadoEm), MONTH(p.atualizadoEm)
           """)
    List<Object[]> contarConcluidosPorMes(@Param("desde") java.time.LocalDateTime desde);

    /** Usado antes de excluir um paciente, para não deixar histórico clínico órfão (ver PacienteService#excluir). */
    boolean existsByPacienteId(Long pacienteId);

    /**
     * Bug médio de revisão corrigido: nada impedia abrir DOIS protocolos para
     * o mesmo paciente + mesmo procedimento enquanto o primeiro ainda estava
     * aguardando/agendado/em andamento -- duplicava a entrada na fila (ver
     * ProtocoloService#criar).
     */
    boolean existsByPacienteIdAndProcedimentoIdAndStatusIn(Long pacienteId, Long procedimentoId, List<StatusProtocolo> status);

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
     * Bug médio de revisão corrigido: a cota (item 3.3) media o mês pela
     * data de CRIAÇÃO do protocolo (`dataInclusao`), não pela data real do
     * atendimento (`dataPrevista`). Um protocolo criado em janeiro mas
     * agendado para março contava contra a cota de janeiro, e a cota de
     * março nunca via esse protocolo -- tanto a checagem de disponibilidade
     * quanto o indicador de ocupação usam esta query em vez da antiga.
     */
    List<Protocolo> findByUnidadeSaudeIdAndProcedimento_EspecialidadeAndDataPrevistaBetween(
            Long unidadeSaudeId, String especialidade, java.time.LocalDate inicio, java.time.LocalDate fim);

    /**
     * Protocolos ainda aguardando atendimento que ainda não geraram alerta de
     * SLA -- usado pela varredura diária do {@code SlaAlertaService}. Desde
     * que o SLA passou a ser configurável por especialidade/procedimento
     * (ver {@code SlaConfiguracaoService}), o filtro "já ultrapassou o
     * limite" não é mais uma data única e por isso não pode mais ser feito
     * em SQL -- o processor aplica {@code SlaConfiguracaoService#estaAtrasado}
     * sobre esta lista em memória.
     */
    List<Protocolo> findByStatusAndAlertaSlaEnviadoEmIsNull(StatusProtocolo status);

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

    /**
     * Quantos protocolos já receberam o lembrete de agendamento e estão em
     * cada situação de confirmação de presença (ver LembreteAgendamentoService/
     * ProtocoloConfirmacaoService) -- usado pelo indicador de taxa de
     * confirmação do painel administrativo.
     */
    long countByPresencaConfirmacao(PresencaConfirmacao presencaConfirmacao);
}
