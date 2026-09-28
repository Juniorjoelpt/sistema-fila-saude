package br.com.filasaude.service;

import br.com.filasaude.domain.Paciente;
import br.com.filasaude.domain.Procedimento;
import br.com.filasaude.domain.Protocolo;
import br.com.filasaude.domain.enums.CategoriaPrioridade;
import br.com.filasaude.domain.enums.StatusProtocolo;
import br.com.filasaude.repository.ProtocoloRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static br.com.filasaude.support.Fixtures.paciente;
import static br.com.filasaude.support.Fixtures.procedimento;
import static br.com.filasaude.support.Fixtures.protocolo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Motor de priorização da fila (item 3.2): categoria de prioridade primeiro,
 * FIFO (data de inclusão, depois criação) dentro da mesma categoria --
 * proteção contra "fura-filas" (item 3.5).
 */
@ExtendWith(MockitoExtension.class)
class FilaPriorizacaoServiceTest {

    @Mock
    private ProtocoloRepository protocoloRepository;

    private final Paciente paciente = paciente(1L, "Paciente Teste");
    private final Procedimento procedimento = procedimento(1L, "Consulta Cardiologia", "Cardiologia");

    private FilaPriorizacaoService service() {
        return new FilaPriorizacaoService(protocoloRepository);
    }

    private Protocolo com(Long id, CategoriaPrioridade categoria, LocalDate dataInclusao, LocalDateTime criadoEm) {
        Protocolo p = protocolo(id, paciente, procedimento);
        p.setCategoriaPrioridade(categoria);
        p.setDataInclusao(dataInclusao);
        p.setCriadoEm(criadoEm);
        return p;
    }

    @Test
    void urgenciaVemAntesDeNormalMesmoTendoEntradoDepois() {
        LocalDate hoje = LocalDate.now();
        Protocolo normalAntigo = com(1L, CategoriaPrioridade.NORMAL, hoje.minusDays(10), LocalDateTime.now().minusDays(10));
        Protocolo urgenciaRecente = com(2L, CategoriaPrioridade.URGENCIA, hoje, LocalDateTime.now());

        when(protocoloRepository.findByStatus(StatusProtocolo.AGUARDANDO))
                .thenReturn(List.of(normalAntigo, urgenciaRecente));

        List<Protocolo> fila = service().filaOrdenada();

        assertThat(fila).extracting(Protocolo::getId).containsExactly(2L, 1L);
    }

    @Test
    void dentroDaMesmaCategoriaOrdenaPorFifo() {
        LocalDate hoje = LocalDate.now();
        Protocolo maisNovo = com(1L, CategoriaPrioridade.NORMAL, hoje, LocalDateTime.now());
        Protocolo maisAntigo = com(2L, CategoriaPrioridade.NORMAL, hoje.minusDays(3), LocalDateTime.now().minusDays(3));
        Protocolo intermediario = com(3L, CategoriaPrioridade.NORMAL, hoje.minusDays(1), LocalDateTime.now().minusDays(1));

        when(protocoloRepository.findByStatus(StatusProtocolo.AGUARDANDO))
                .thenReturn(List.of(maisNovo, maisAntigo, intermediario));

        List<Protocolo> fila = service().filaOrdenada();

        assertThat(fila).extracting(Protocolo::getId).containsExactly(2L, 3L, 1L);
    }

    @Test
    void posicaoNaFilaERetornaVazioParaProtocoloQueNaoEstaAguardando() {
        LocalDate hoje = LocalDate.now();
        Protocolo p1 = com(1L, CategoriaPrioridade.URGENCIA, hoje, LocalDateTime.now());
        Protocolo p2 = com(2L, CategoriaPrioridade.NORMAL, hoje, LocalDateTime.now());

        when(protocoloRepository.findByStatus(StatusProtocolo.AGUARDANDO)).thenReturn(List.of(p1, p2));

        Optional<Integer> posicaoP2 = service().posicaoNaFila(2L);
        Optional<Integer> posicaoInexistente = service().posicaoNaFila(999L);

        assertThat(posicaoP2).contains(2);
        assertThat(posicaoInexistente).isEmpty();
    }
}
