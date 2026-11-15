public class App {
    // ---------------------- CONSTANTES ----------------------
    static final int MAX_PROCESSOS = 8; // número máximo de processos
    static final long SEED = 42; // semente fixa
    static final int CPU_MIN = 8; // faixa de tempo de CPU por processo
    static final int CPU_MAX = 25;

    // Duração mínima e máxima de cada tipo de I/O
    static final int DUR_DISCO_MIN = 3, DUR_DISCO_MAX = 7;
    static final int DUR_FITA_MIN = 4, DUR_FITA_MAX = 9;
    static final int DUR_IMPR_MIN = 5, DUR_IMPR_MAX = 10;

    // ---------------------- ENUMERAÇÕES ----------------------
    static final int PRIORIDADE_BAIXA = 0, PRIORIDADE_ALTA = 1;

    static final int STATUS_NOVO = 0, STATUS_PRONTO = 1, STATUS_EXEC = 2,
            STATUS_BLOQ = 3, STATUS_FIM = 4;
    static final String[] NOME_STATUS = { "NOVO", "PRONTO", "EXECUTANDO", "BLOQUEADO", "FINALIZADO" };

    static final int IO_NENHUM = 0, IO_DISCO = 1, IO_FITA = 2, IO_IMPRESSORA = 3;
    static final String[] NOME_IO = { "Nenhum", "Disco", "Fita", "Impressora" };

    // ---------------------- PCB (Product ) ----------------------
    static int[] pid = new int[MAX_PROCESSOS]; // ID do processo
    static int[] ppid = new int[MAX_PROCESSOS]; // ID do processo pai (-1 = sem pai)
    static int[] prioridade = new int[MAX_PROCESSOS]; // ALTA ou BAIXA
    static int[] status = new int[MAX_PROCESSOS]; // NOVO, PRONTO, EXEC, BLOQ, FIM
    static int[] cpuTotal = new int[MAX_PROCESSOS]; // tempo total de CPU requerido
    static int[] cpuRestante = new int[MAX_PROCESSOS]; // tempo de CPU que ainda falta
    static int[] tipoIo = new int[MAX_PROCESSOS]; // próximo I/O / I/O atual
    static int[] ioRestante = new int[MAX_PROCESSOS]; // tempo restante do I/O

    static int N = 0; // número de processos criados
    static int tempo = 0; // relógio global da simulação

    // ---------------------- GERADOR ALEATÓRIO ----------------------
    static long estadoRnd = 1;

    // Ponto de partida da sequencia
    static void semente(long s) {
        estadoRnd = Math.abs(s % 2147483648L); // limite do long
    }

    // Calcula o proximo numero da sequencia
    static int rand() {
        estadoRnd = (estadoRnd * 1103515245L + 12345L) % 2147483648L;
        return (int) (estadoRnd / 65536);
    }

    // Gera um valor aleatório entre [min, max]
    static int rndRange(int min, int max) {
        return min + rand() % (max - min + 1);
    }

    // Define a duração da operação de I/O de acordo com o tipo
    static int duracaoIo(int tipo) {
        switch (tipo) {
            case IO_DISCO:
                return rndRange(DUR_DISCO_MIN, DUR_DISCO_MAX);
            case IO_FITA:
                return rndRange(DUR_FITA_MIN, DUR_FITA_MAX);
            case IO_IMPRESSORA:
                return rndRange(DUR_IMPR_MIN, DUR_IMPR_MAX);
            default:
                return 0;
        }
    }

    // ---------------------- PCB ----------------------

    // Preenche o PCB do processo i
    static void criarPCB(int i, int pai) {
        pid[i] = i;
        ppid[i] = pai;
        prioridade[i] = PRIORIDADE_ALTA; // todos começam em ALTA
        status[i] = STATUS_NOVO;
        cpuTotal[i] = rndRange(CPU_MIN, CPU_MAX);
        cpuRestante[i] = cpuTotal[i];
        tipoIo[i] = IO_NENHUM;
        ioRestante[i] = 0;
    }

    static void imprimirPCB(int i) {
        System.out.printf("P%-3d PPID=%-3d %-10s %-5s CPU=%2d/%-2d IO=%s(%d)%n",
                pid[i], ppid[i], NOME_STATUS[status[i]],
                prioridade[i] == PRIORIDADE_ALTA ? "ALTA" : "BAIXA",
                cpuRestante[i], cpuTotal[i], NOME_IO[tipoIo[i]], ioRestante[i]);
    }

    static void imprimirTabela() {
        System.out.println("---------------- TABELA DE PROCESSOS ----------------");
        for (int i = 0; i < N; i++)
            imprimirPCB(i);
        System.out.println("-----------------------------------------------------");
    }

    // ---------------------- CRIAÇÃO DE PROCESSOS ----------------------

    static void gerarProcessos() {
        semente(SEED);
        N = MAX_PROCESSOS;

        for (int i = 0; i < N; i++) {
            int pai = (i == 0) ? -1 : 0;
            criarPCB(i, pai);

            status[i] = STATUS_PRONTO;
            filaPush(FILA_ALTA, pid[i]);

            LogsMetricas.registrarChegada(i);
        }
    }

    // 2. FILAS

    static final int FILA_ALTA = 0;

    static boolean filaPush(int fila, int p) {
        return true;
    }

    public static void main(String[] args) {
        LogsMetricas.iniciar(
                Configuracao.QUANTUM_ALTA,
                Configuracao.QUANTUM_BAIXA,
                MAX_PROCESSOS,
                SEED);

        gerarProcessos();
        imprimirTabela();
    }
}