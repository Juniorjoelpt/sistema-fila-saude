package br.com.filasaude.service;

/**
 * Validação dos dígitos verificadores de CPF e CNS (Cartão Nacional de
 * Saúde). Antes só o tamanho era conferido, e um CPF como 12345678900 recebia
 * "formato válido". Recebe apenas dígitos (quem chama já removeu a máscara).
 *
 * Não confirma que o documento EXISTE (isso é papel do CADSUS, ainda sem
 * integração real) -- só que ele é matematicamente possível.
 */
public final class DocumentoValidador {

    private DocumentoValidador() {
    }

    public static boolean cpfValido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}")) {
            return false;
        }
        // 000.000.000-00, 111.111.111-11 etc. passam na conta, mas não são CPFs.
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digitoCpf(cpf, 9) == cpf.charAt(9) - '0' && digitoCpf(cpf, 10) == cpf.charAt(10) - '0';
    }

    private static int digitoCpf(String cpf, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    public static boolean cnsValido(String cns) {
        if (cns == null || !cns.matches("\\d{15}")) {
            return false;
        }
        char inicial = cns.charAt(0);
        if (inicial == '1' || inicial == '2') {
            return cnsDefinitivoValido(cns);
        }
        if (inicial == '7' || inicial == '8' || inicial == '9') {
            return somaPonderadaCns(cns, 15) % 11 == 0;
        }
        return false;
    }

    // CNS definitivo (começa com 1 ou 2): 11 dígitos do PIS + "000"/"001" + dígito verificador.
    private static boolean cnsDefinitivoValido(String cns) {
        String pis = cns.substring(0, 11);
        int soma = somaPonderadaCns(pis, 11);
        int resto = soma % 11;
        int dv = 11 - resto;
        if (dv == 11) {
            dv = 0;
        }
        String esperado;
        if (dv == 10) {
            soma += 2;
            resto = soma % 11;
            dv = 11 - resto;
            esperado = pis + "001" + dv;
        } else {
            esperado = pis + "000" + dv;
        }
        return esperado.equals(cns);
    }

    private static int somaPonderadaCns(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * (15 - i);
        }
        return soma;
    }
}
