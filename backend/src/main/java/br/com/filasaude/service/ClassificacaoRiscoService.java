package br.com.filasaude.service;

import br.com.filasaude.domain.enums.CategoriaPrioridade;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

/**
 * Classificação de risco assistida (item do roadmap). As categorias ESPECIAL
 * (80+) e LEGAL (60+, PCD, gestante) têm critério OBJETIVO e verificável a
 * partir do próprio cadastro do paciente -- diferente de URGENCIA (julgamento
 * clínico) e JUDICIAL (depende de um processo/mandado real), que exigem
 * decisão humana e por isso nunca são sugeridas automaticamente aqui.
 *
 * Esta é a mesma regra que {@code ProtocoloService#validarCriterioPrioridade}
 * usa para REJEITAR uma categoria incompatível com o paciente -- centralizada
 * aqui para não duplicar a lógica de elegibilidade em dois lugares (o
 * validador consome {@link #elegivel}, e a sugestão ao operador consome
 * {@link #sugerir}).
 */
@Service
public class ClassificacaoRiscoService {

    public record ClassificacaoSugerida(CategoriaPrioridade categoriaSugerida, String motivo) {
    }

    public ClassificacaoSugerida sugerir(LocalDate dataNascimento, boolean pcd, boolean gestante) {
        Integer idade = calcularIdade(dataNascimento);

        if (idade != null && idade >= 80) {
            return new ClassificacaoSugerida(CategoriaPrioridade.ESPECIAL,
                    "paciente com " + idade + " anos (critério: 80+)");
        }

        if (idade != null && idade >= 60 || pcd || gestante) {
            StringBuilder motivo = new StringBuilder("paciente ");
            boolean primeiro = true;
            if (idade != null && idade >= 60) {
                motivo.append("com ").append(idade).append(" anos (60+)");
                primeiro = false;
            }
            if (pcd) {
                motivo.append(primeiro ? "" : " e ").append("com deficiência (PCD)");
                primeiro = false;
            }
            if (gestante) {
                motivo.append(primeiro ? "" : " e ").append("gestante");
            }
            return new ClassificacaoSugerida(CategoriaPrioridade.LEGAL, motivo.toString());
        }

        return new ClassificacaoSugerida(CategoriaPrioridade.NORMAL,
                "nenhum critério objetivo de Especial/Legal identificado -- "
                        + "avalie manualmente se há urgência clínica ou processo judicial");
    }

    /**
     * Usado por {@code ProtocoloService#validarCriterioPrioridade} para
     * confirmar que o paciente de fato atende ao critério da categoria antes
     * de permitir que o protocolo seja salvo com ela.
     */
    public boolean elegivel(CategoriaPrioridade categoria, LocalDate dataNascimento, boolean pcd, boolean gestante) {
        Integer idade = calcularIdade(dataNascimento);
        if (categoria == CategoriaPrioridade.ESPECIAL) {
            return idade != null && idade >= 80;
        }
        if (categoria == CategoriaPrioridade.LEGAL) {
            return (idade != null && idade >= 60) || pcd || gestante;
        }
        return true;
    }

    /** Sem data de nascimento cadastrada, a idade não pode ser confirmada -- retorna null, nunca um palpite. */
    private Integer calcularIdade(LocalDate dataNascimento) {
        if (dataNascimento == null) {
            return null;
        }
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }
}
