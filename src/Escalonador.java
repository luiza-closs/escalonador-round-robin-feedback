import java.util.Arrays;

/** Escalonador Round Robin com Feedback: fila ALTA tem preferencia sobre a BAIXA. */
final class Escalonador {
    // Plano de I/O por processo: pede I/O ao consumir ioApos[i][k] de CPU, no dispositivo ioDisp[i][k].
    static final int MAX_IO_POR_PROCESSO = 3;
    private static final int[][] ioApos = new int[App.MAX_PROCESSOS][MAX_IO_POR_PROCESSO];
    private static final int[][] ioDisp = new int[App.MAX_PROCESSOS][MAX_IO_POR_PROCESSO];
    private static final int[] qtdIo = new int[App.MAX_PROCESSOS];
    private static final int[] proxIo = new int[App.MAX_PROCESSOS];

    // Linha do tempo da CPU: segmentos [inicio, fim) de um PID (-1 = ociosa).
    private static int[] segPid = new int[64];
    private static int[] segInicio = new int[64];
    private static int[] segFim = new int[64];
    private static int segmentos = 0;

    private Escalonador() {}

    /** Roda a simulacao ate todos os processos finalizarem. */
    static void executar() {
        int finalizados = 0;
        segmentos = 0;
        EntradaSaida.inicializar();
        planejarIo();
        imprimirPlanoIo();

        while (finalizados < App.N) {
            int i = escolherProximo();

            if (i < 0) { // nenhum processo pronto: CPU ociosa ate um I/O terminar
                int espera = menorIoRestante();
                if (espera == 0) {
                    break;
                }
                registrarSegmento(-1, App.tempo, App.tempo + espera);
                LogsMetricas.registrarCpuOciosa(espera);
                for (int k = 0; k < espera; k++) {
                    App.tempo++;
                    EntradaSaida.atualizar();
                }
                continue;
            }

            if (executarFatia(i)) {
                finalizados++;
            }
        }
    }

    // Retorna o PID da fila ALTA ou, se vazia, da BAIXA; -1 se ambas vazias.
    private static int escolherProximo() {
        if (!Filas.vazia(Filas.FILA_ALTA)) {
            return Filas.remover(Filas.FILA_ALTA);
        }
        if (!Filas.vazia(Filas.FILA_BAIXA)) {
            return Filas.remover(Filas.FILA_BAIXA);
        }
        return -1;
    }

    // Executa o processo por ate um quantum. Retorna true se finalizou.
    private static boolean executarFatia(int i) {
        int quantum = App.prioridade[i] == App.PRIORIDADE_ALTA
                ? Configuracao.QUANTUM_ALTA : Configuracao.QUANTUM_BAIXA;
        int fatia = Math.min(quantum, App.cpuRestante[i]);

        App.status[i] = App.STATUS_EXEC;
        LogsMetricas.registrarExecucao(i);
        int inicio = App.tempo;

        for (int t = 1; t <= fatia; t++) {
            App.tempo++;
            App.cpuRestante[i]--;
            EntradaSaida.atualizar();

            if (App.cpuRestante[i] == 0) { // finalizou
                registrarSegmento(i, inicio, App.tempo);
                App.status[i] = App.STATUS_FIM;
                LogsMetricas.registrarFinalizacao(i);
                return true;
            }
            if (pediuIoAgora(i)) { // bloqueou por I/O
                registrarSegmento(i, inicio, App.tempo);
                return false;
            }
        }

        // quantum esgotado: preempcao, volta para a fila BAIXA
        registrarSegmento(i, inicio, App.tempo);
        App.status[i] = App.STATUS_PRONTO;
        App.prioridade[i] = App.PRIORIDADE_BAIXA;
        Filas.inserir(Filas.FILA_BAIXA, App.pid[i]);
        LogsMetricas.registrarPreempcao(i);
        return false;
    }

    // ---------------------- I/O ----------------------

    // Sorteia 0 a MAX_IO_POR_PROCESSO pedidos por processo, em pontos crescentes
    // entre 1 e cpuTotal-1 (o k-esimo pedido cai no k-esimo trecho da CPU).
    private static void planejarIo() {
        for (int i = 0; i < App.N; i++) {
            int qtd = Math.min(App.rand() % (MAX_IO_POR_PROCESSO + 1), App.cpuTotal[i] - 1);
            int trecho = qtd > 0 ? (App.cpuTotal[i] - 1) / qtd : 0;
            qtdIo[i] = qtd;
            proxIo[i] = 0;
            for (int k = 0; k < qtd; k++) {
                ioApos[i][k] = k * trecho + 1 + App.rand() % trecho;
                ioDisp[i][k] = App.IO_DISCO + App.rand() % 3;
            }
        }
    }

    // Se o processo chegou ao proximo ponto do plano, pede I/O e retorna true.
    private static boolean pediuIoAgora(int i) {
        int k = proxIo[i];
        if (k >= qtdIo[i] || App.cpuTotal[i] - App.cpuRestante[i] != ioApos[i][k]) {
            return false;
        }
        proxIo[i]++;
        return EntradaSaida.solicitar(i, ioDisp[i][k]);
    }

    // Menor tempo de I/O restante entre os bloqueados (0 se nenhum).
    private static int menorIoRestante() {
        int menor = 0;
        for (int i = 0; i < App.N; i++) {
            if (App.status[i] == App.STATUS_BLOQ && (menor == 0 || App.ioRestante[i] < menor)) {
                menor = App.ioRestante[i];
            }
        }
        return menor;
    }

    private static void imprimirPlanoIo() {
        System.out.println("\n--- Plano de I/O (apos X unidades de CPU) ---");
        for (int i = 0; i < App.N; i++) {
            StringBuilder linha = new StringBuilder("P" + App.pid[i] + ": ");
            if (qtdIo[i] == 0) {
                linha.append("sem I/O");
            }
            for (int k = 0; k < qtdIo[i]; k++) {
                linha.append(k > 0 ? ", " : "")
                        .append(App.NOME_IO[ioDisp[i][k]]).append(" @").append(ioApos[i][k]);
            }
            System.out.println(linha);
        }
        System.out.println();
    }

    // ---------------------- LINHA DO TEMPO ----------------------

    private static void registrarSegmento(int pid, int inicio, int fim) {
        if (segmentos > 0 && segPid[segmentos - 1] == pid && segFim[segmentos - 1] == inicio) {
            segFim[segmentos - 1] = fim; // junta fatias seguidas do mesmo processo
            return;
        }
        if (segmentos == segPid.length) {
            segPid = Arrays.copyOf(segPid, segmentos * 2);
            segInicio = Arrays.copyOf(segInicio, segmentos * 2);
            segFim = Arrays.copyOf(segFim, segmentos * 2);
        }
        segPid[segmentos] = pid;
        segInicio[segmentos] = inicio;
        segFim[segmentos] = fim;
        segmentos++;
    }

    static void imprimirLinhaDoTempo() {
        System.out.println("\n--- Linha do tempo da CPU ---");
        for (int s = 0; s < segmentos; s++) {
            String quem = segPid[s] < 0 ? "OCIOSA" : "P" + segPid[s];
            System.out.printf("[%03d - %03d) %s%n", segInicio[s], segFim[s], quem);
        }
    }
}
