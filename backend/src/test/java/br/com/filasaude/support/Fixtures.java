package br.com.filasaude.support;

import br.com.filasaude.domain.HorarioAgenda;
import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.UnidadeSaude;
import br.com.filasaude.domain.enums.CategoriaPrioridade;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.domain.enums.TipoProcedimento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Fábrica de objetos de domínio prontos para os testes -- construídos como
 * POJOs comuns (sem Hibernate/persistência envolvida), então nenhum dos
 * cuidados de lazy-loading que valem em produção se aplicam aqui: os testes
 * de serviço trabalham inteiramente em memória, com os repositórios
 * mockados (Mockito).
 */
public final class Fixtures {

    private Fixtures() {
    }

    public static Paciente paciente(Long id, String nome) {
        return Paciente.builder()
                .id(id)
                .nome(nome)
                .email(nome.toLowerCase().replace(" ", ".") + "@example.com")
                .telefone("86999999999")
                .build();
    }

    public static Procedimento procedimento(Long id, String nome, String especialidade) {
        return Procedimento.builder()
                .id(id)
                .nome(nome)
                .tipo(TipoProcedimento.CONSULTA)
                .especialidade(especialidade)
                .ativo(true)
                .build();
    }

    public static UnidadeSaude unidadeSaude(Long id, String nome) {
        return UnidadeSaude.builder()
                .id(id)
                .nome(nome)
                .ativo(true)
                .build();
    }

    public static HorarioAgenda horarioAgenda(Long id, UnidadeSaude unidade, String especialidade,
                                                LocalDate data, LocalTime hora, int capacidade) {
        return HorarioAgenda.builder()
                .id(id)
                .unidadeSaude(unidade)
                .especialidade(especialidade)
                .data(data)
                .horaInicio(hora)
                .capacidadeTotal(capacidade)
                .build();
    }

    /** Protocolo AGUARDANDO padrão, pronto para customizar via os setters do Lombok. */
    public static Protocolo protocolo(Long id, Paciente paciente, Procedimento procedimento) {
        return Protocolo.builder()
                .id(id)
                .numeroProtocolo("PROTO-" + id)
                .paciente(paciente)
                .procedimento(procedimento)
                .categoriaPrioridade(CategoriaPrioridade.NORMAL)
                .status(StatusProtocolo.AGUARDANDO)
                .dataSolicitacao(LocalDate.now())
                .dataInclusao(LocalDate.now())
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }
}
