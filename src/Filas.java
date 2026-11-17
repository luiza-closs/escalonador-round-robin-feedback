/**
 * Filas FIFO do simulador. Cada processo pode estar em apenas uma fila por vez.
 * Os elementos armazenados são os PIDs definidos em App.
 *
 * "final" impede que outra classe herde de Filas. Não torna os dados imutáveis;
 * aqui apenas deixa explícito que esta classe utilitária não é base de herança.
 */
final class Filas {
    // Códigos usados por App e pelos demais módulos para escolher uma fila.
    static final int FILA_ALTA = 0;
    static final int FILA_BAIXA = 1;
    static final int FILA_IO = 2;
    private static final int QUANTIDADE_FILAS = 3;

    // Cada linha do array é uma fila. Inicio aponta para o primeiro elemento;
    // quantidade diz quantos elementos estão ocupando essa fila.
    private static final int[][] elementos = new int[QUANTIDADE_FILAS][App.MAX_PROCESSOS];
    private static final int[] inicio = new int[QUANTIDADE_FILAS];
    private static final int[] quantidade = new int[QUANTIDADE_FILAS];

    // Construtor privado: usamos Filas.inserir(...), sem criar "new Filas()".
    private Filas() {
        // Classe utilitária: não deve ser instanciada.
    }

    /** Insere o PID no fim da fila. Retorna false se a fila estiver cheia ou os dados forem inválidos. */
    static boolean inserir(int fila, int pid) {
        if (!filaValida(fila) || !pidValido(pid) || quantidade[fila] == App.MAX_PROCESSOS
                || contem(pid)) {
            return false;
        }

        // O módulo faz o índice voltar ao começo do array quando chega ao fim:
        // assim o espaço liberado por remoções pode ser reutilizado (fila circular).
        int fim = (inicio[fila] + quantidade[fila]) % App.MAX_PROCESSOS;
        elementos[fila][fim] = pid;
        quantidade[fila]++;
        return true;
    }

    /** Remove e retorna o PID mais antigo. Retorna -1 quando a fila está vazia ou é inválida. */
    static int remover(int fila) {
        if (!filaValida(fila) || quantidade[fila] == 0) {
            return -1;
        }

        // FIFO: removemos o primeiro que entrou e avançamos o início circularmente.
        int pid = elementos[fila][inicio[fila]];
        inicio[fila] = (inicio[fila] + 1) % App.MAX_PROCESSOS;
        quantidade[fila]--;
        return pid;
    }

    static boolean vazia(int fila) {
        return !filaValida(fila) || quantidade[fila] == 0;
    }

    static int tamanho(int fila) {
        return filaValida(fila) ? quantidade[fila] : 0;
    }

    /** Mostra uma fila na ordem em que os elementos serão removidos. */
    static void imprimir(int fila) {
        if (!filaValida(fila)) {
            System.out.println("Fila inválida");
            return;
        }

        System.out.print(nome(fila) + ": [");
        for (int posicao = 0; posicao < quantidade[fila]; posicao++) {
            if (posicao > 0) {
                System.out.print(", ");
            }
            int indice = (inicio[fila] + posicao) % App.MAX_PROCESSOS;
            System.out.print("P" + elementos[fila][indice]);
        }
        System.out.println("]");
    }

    /** Mostra as três filas, incluindo as que ainda estão vazias. */
    static void imprimirTodas() {
        imprimir(FILA_ALTA);
        imprimir(FILA_BAIXA);
        imprimir(FILA_IO);
    }

    /** Esvazia uma fila; útil ao reiniciar uma simulação na mesma execução. */
    static void limpar(int fila) {
        if (filaValida(fila)) {
            inicio[fila] = 0;
            quantidade[fila] = 0;
        }
    }

    static void limparTodas() {
        for (int fila = 0; fila < QUANTIDADE_FILAS; fila++) {
            limpar(fila);
        }
    }

    // Evita que um mesmo processo seja inserido em duplicidade ou em duas filas.
    private static boolean contem(int pid) {
        for (int fila = 0; fila < QUANTIDADE_FILAS; fila++) {
            for (int posicao = 0; posicao < quantidade[fila]; posicao++) {
                int indice = (inicio[fila] + posicao) % App.MAX_PROCESSOS;
                if (elementos[fila][indice] == pid) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean filaValida(int fila) {
        return fila >= 0 && fila < QUANTIDADE_FILAS;
    }

    private static boolean pidValido(int pid) {
        return pid >= 0 && pid < App.MAX_PROCESSOS;
    }

    private static String nome(int fila) {
        switch (fila) {
            case FILA_ALTA:
                return "Fila alta";
            case FILA_BAIXA:
                return "Fila baixa";
            case FILA_IO:
                return "Fila de I/O";
            default:
                return "Fila inválida";
        }
    }
}
