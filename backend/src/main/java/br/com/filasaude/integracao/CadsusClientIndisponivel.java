package br.com.filasaude.integracao;

import br.com.filasaude.domain.enums.TipoIntegracao;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Única implementação de {@link CadsusClient} até a homologação/acesso real
 * ao webservice do DATASUS (ver Javadoc da interface) -- sempre lança
 * {@link IntegracaoNaoConfiguradaException}. Registrada como @Service para
 * que {@code ValidacaoCadastralService} já possa depender da interface
 * normalmente; no dia em que a integração real existir, basta trocar esta
 * classe por uma que chame o webservice, sem mudar o restante do sistema.
 */
@Service
public class CadsusClientIndisponivel implements CadsusClient {

    @Override
    public Optional<UsuarioCadsus> consultarPorCpf(String cpf) {
        throw new IntegracaoNaoConfiguradaException(TipoIntegracao.CADSUS);
    }

    @Override
    public Optional<UsuarioCadsus> consultarPorCns(String cns) {
        throw new IntegracaoNaoConfiguradaException(TipoIntegracao.CADSUS);
    }
}
