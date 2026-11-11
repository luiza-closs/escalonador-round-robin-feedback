import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Leitura central da configuração comum do simulador. */
public final class Configuracao {
    private static final Map<String, String> ARQUIVO = carregarArquivo();

    public static final long SEMENTE = obterLongo("SEMENTE", 42L);
    public static final int QUANTUM_ALTA = obterInteiro("QUANTUM_ALTA", 2);
    public static final int QUANTUM_BAIXA = obterInteiro("QUANTUM_BAIXA", 4);
    public static final int QUANTIDADE_PROCESSOS = obterInteiro("QUANTIDADE_PROCESSOS", 10);

    private Configuracao() {
        // Classe utilitária: não deve ser instanciada.
    }

    private static Map<String, String> carregarArquivo() {
        Map<String, String> valores = new HashMap<>();
        Path arquivo = Path.of(".env");
        if (!Files.isRegularFile(arquivo)) {
            return valores;
        }

        try {
            List<String> linhas = Files.readAllLines(arquivo);
            for (String linha : linhas) {
                String conteudo = linha.trim();
                if (conteudo.isEmpty() || conteudo.startsWith("#")) {
                    continue;
                }
                int separador = conteudo.indexOf('=');
                if (separador <= 0) {
                    continue;
                }
                String chave = conteudo.substring(0, separador).trim();
                String valor = conteudo.substring(separador + 1).trim();
                valores.put(chave, valor);
            }
        } catch (IOException erro) {
            System.err.println("Aviso: não foi possível ler .env: " + erro.getMessage());
        }
        return valores;
    }

    private static String obter(String chave, String padrao) {
        String ambiente = System.getenv(chave);
        if (ambiente != null && !ambiente.isBlank()) {
            return ambiente.trim();
        }
        return ARQUIVO.getOrDefault(chave, padrao);
    }

    private static int obterInteiro(String chave, int padrao) {
        try {
            return Integer.parseInt(obter(chave, Integer.toString(padrao)));
        } catch (NumberFormatException erro) {
            System.err.println("Aviso: " + chave + " inválido; usando " + padrao + ".");
            return padrao;
        }
    }

    private static long obterLongo(String chave, long padrao) {
        try {
            return Long.parseLong(obter(chave, Long.toString(padrao)));
        } catch (NumberFormatException erro) {
            System.err.println("Aviso: " + chave + " inválido; usando " + padrao + ".");
            return padrao;
        }
    }
}
