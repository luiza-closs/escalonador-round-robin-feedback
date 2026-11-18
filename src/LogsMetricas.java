import java.util.Arrays;
import java.util.Locale;

/** 
 * Registra os eventos do escalonador e calcula as métricas da simulação.
 * Os métodos são estáticos e usam o relógio e os vetores do PCB de App
 */
final class LogsMetricas {
    // O índice i corresponde à posição do processo nos vetores do PCB.
    private static final int[] chegada = new int[App.MAX_PROCESSOS];
    private static final int[] entradaNaFila = new int[App.MAX_PROCESSOS];
    private static final long[] espera = new long[App.MAX_PROCESSOS];
    private static final int[] fim = new int[App.MAX_PROCESSOS];
    private static final long[] solicitacoesIo = new long[App.NOME_IO.length];

    private static int inicio;
    private static long preempcoes;
    private static long cpuOciosa;

    private LogsMetricas() {}

    // Prepara as métricas da execução.
    static void iniciar(int quantumAlta, int quantumBaixa, int quantidade, long semente) {
        inicio = App.tempo;
        preempcoes = 0;
        cpuOciosa = 0;
        Arrays.fill(espera, 0);
        Arrays.fill(fim, -1);
        Arrays.fill(solicitacoesIo, 0);
        System.out.printf("[config] quantum alta=%d | quantum baixa=%d | processos=%d | seed=%d%n",
                quantumAlta, quantumBaixa, quantidade, semente);
    }

    // Formata a mensagem do evento e acrescenta o tempo simulado, como [t=003].
    private static void log(String formato, Object... argumentos) {
        System.out.printf("[t=%03d] %s%n", App.tempo, String.format(formato, argumentos));
    }

    // Registra a chegada depois que o processo entra na fila de alta prioridade.
    static void registrarChegada(int i) {
        chegada[i] = App.tempo;
        entradaNaFila[i] = App.tempo;
        log("Criado P%d (PPID=%d, CPU=%d) -> FILA ALTA",
                App.pid[i], App.ppid[i], App.cpuTotal[i]);
    }

    static void registrarExecucao(int i) {
        espera[i] += App.tempo - entradaNaFila[i];
        log("CPU inicia P%d (fila %s, CPU restante=%d)",
                App.pid[i], nomePrioridade(i), App.cpuRestante[i]);
    }

    // Registra o fim do quantum quando o processo é devolvido à fila baixa.
    static void registrarPreempcao(int i) {
        preempcoes++;
        entradaNaFila[i] = App.tempo;
        log("P%d sofreu preempção -> FILA BAIXA", App.pid[i]);
    }

    // Conta a solicitação de I/O no momento em que o processo é bloqueado.
    static void registrarBloqueio(int i, int dispositivo, int duracao) {
        solicitacoesIo[dispositivo]++;
        log("P%d solicitou I/O %s por %d unidade(s)", App.pid[i],
                App.NOME_IO[dispositivo], duracao);
    }


    static void registrarRetornoIo(int i, int dispositivo) {
        entradaNaFila[i] = App.tempo;
        log("P%d retornou de %s -> FILA %s", App.pid[i],
                App.NOME_IO[dispositivo], nomePrioridade(i));
    }

    // Guarda o instante de conclusão, após contabilizar a última unidade de CPU.
    static void registrarFinalizacao(int i) {
        fim[i] = App.tempo;
        log("P%d finalizado", App.pid[i]);
    }

    // Acumula a duração ociosa. Chamado antes de o relógio avançar esse intervalo.
    static void registrarCpuOciosa(int duracao) {
        cpuOciosa += duracao;
        log("CPU ociosa por %d unidade(s)", duracao);
    }

    // Converte a prioridade numérica do PCB em um texto para o log.
    private static String nomePrioridade(int i) {
        return App.prioridade[i] == App.PRIORIDADE_ALTA ? "ALTA" : "BAIXA";
    }


    // Exibe os resultados
    static void imprimirResumo() {
        int finalizados = 0;
        long somaEspera = 0;
        long somaTurnaround = 0;

        System.out.println("\n--- Resumo por processo ---");
        System.out.printf("%-8s %8s %8s %8s %12s%n", "PID", "Chegada", "Fim", "Espera", "Turnaround");
        for (int i = 0; i < App.N; i++) {
            if (fim[i] < 0) {
                continue;
            }
            int turnaround = fim[i] - chegada[i];
            finalizados++;
            somaEspera += espera[i];
            somaTurnaround += turnaround;
            System.out.printf("P%-7d %8d %8d %8d %12d%n",
                    App.pid[i], chegada[i], fim[i], espera[i], turnaround);
        }

        int tempoTotal = App.tempo - inicio;
        double percentualOcioso = tempoTotal == 0 ? 0 : 100.0 * cpuOciosa / tempoTotal;
        
        System.out.printf("%n[fim] Processos finalizados: %d/%d | tempo total: %d%n",
                finalizados, App.N, tempoTotal);
        System.out.printf("Preempções: %d%n", preempcoes);
        System.out.printf("Solicitações de I/O: Disco=%d | Fita=%d | Impressora=%d%n",
                solicitacoesIo[App.IO_DISCO], solicitacoesIo[App.IO_FITA],
                solicitacoesIo[App.IO_IMPRESSORA]);
        if (finalizados > 0) {
            System.out.printf(Locale.ROOT, "Espera média: %.2f | turnaround médio: %.2f%n",
                    (double) somaEspera / finalizados, (double) somaTurnaround / finalizados);
        } else {
            System.out.println("Espera média e turnaround médio: sem processos finalizados");
        }
        System.out.printf(Locale.ROOT, "CPU ociosa: %d unidade(s) (%.2f%%)%n",
                cpuOciosa, percentualOcioso);
    }
}
