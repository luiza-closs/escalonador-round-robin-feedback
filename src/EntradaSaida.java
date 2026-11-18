/**
 * Parte do I/O: disco, fita e impressora.
 * Quem pede I/O sai da CPU e espera na fila de I/O até o tempo dele acabar.
 * Na volta, disco manda para a fila baixa e fita/impressora para a fila alta.
 */
final class EntradaSaida {
    private static final int[] fimPrevisto = new int[App.MAX_PROCESSOS];
    private static int ultimoTempoAtualizado = -1;

    private EntradaSaida() { }

    /** Reinicia apenas os controles de I/O. Reinicie também as filas e o PCB ao recomeçar a simulação. */
    static void inicializar() {
        for (int i = 0; i < fimPrevisto.length; i++) fimPrevisto[i] = -1;
        ultimoTempoAtualizado = -1;
    }

    /** Solicita uma operação com duração sorteada pelo gerador do PCB. */
    static boolean solicitar(int indice, int dispositivo) {
        if (!dispositivoValido(dispositivo)) return false;
        if (!processoExecutando(indice)) return false;
        return solicitar(indice, dispositivo, App.duracaoIo(dispositivo));
    }

    /** Solicita uma operação com duração definida, útil para testes deterministas. */
    static boolean solicitar(int indice, int dispositivo, int duracao) {
        if (!processoExecutando(indice) || !dispositivoValido(dispositivo) || duracao <= 0
                || App.tempo < 0 || App.tempo > Integer.MAX_VALUE - duracao
                || fimPrevisto[indice] != -1) return false;
        // Inserir na fila ANTES de mudar o PCB: se estiver cheia, nada é alterado.
        if (!Filas.inserir(Filas.FILA_IO, App.pid[indice])) return false;
        App.status[indice] = App.STATUS_BLOQ;
        App.tipoIo[indice] = dispositivo;
        App.ioRestante[indice] = duracao;
        fimPrevisto[indice] = App.tempo + duracao;
        LogsMetricas.registrarBloqueio(indice, dispositivo, duracao);
        return true;
    }

    /**
     * Atualiza durações e conclui as operações terminadas até App.tempo.
     * Cada PID liberado é encaminhado para a fila de prioridade correspondente.
     * Retorna o número de processos desbloqueados.
     *
     * Se a fila de destino estiver cheia, o processo continua bloqueado
     * na fila I/O, podendo ser liberado em uma chamada posterior.
     */
    static int atualizar() {
        if (App.tempo < 0) throw new IllegalStateException("Relógio negativo");
        if (ultimoTempoAtualizado > App.tempo)
            throw new IllegalStateException("O relógio voltou: reinicie o módulo I/O");
        ultimoTempoAtualizado = App.tempo;
        int total = Filas.tamanho(Filas.FILA_IO);
        int concluidos = 0;
        for (int k = 0; k < total; k++) {
            int pid = Filas.remover(Filas.FILA_IO);
            if (pid < 0 || pid >= App.N || App.pid[pid] != pid)
                throw new IllegalStateException("PID inválido na fila I/O: " + pid);
            int i = pid; // O gerador atual atribui pid[i] = i.
            if (App.status[i] != App.STATUS_BLOQ || fimPrevisto[i] < 0)
                throw new IllegalStateException("PCB inconsistente na fila I/O: P" + pid);
            App.ioRestante[i] = Math.max(0, fimPrevisto[i] - App.tempo);
            int destino = App.tipoIo[i] == App.IO_DISCO ? Filas.FILA_BAIXA : Filas.FILA_ALTA;
            if (App.ioRestante[i] == 0 && Filas.inserir(destino, pid)) {
                int dispositivo = App.tipoIo[i];
                App.prioridade[i] = destino == Filas.FILA_ALTA
                        ? App.PRIORIDADE_ALTA : App.PRIORIDADE_BAIXA;
                App.status[i] = App.STATUS_PRONTO;
                App.tipoIo[i] = App.IO_NENHUM;
                fimPrevisto[i] = -1;
                LogsMetricas.registrarRetornoIo(i, dispositivo);
                concluidos++;
            } else if (!Filas.inserir(Filas.FILA_IO, pid)) {
                throw new IllegalStateException("Falha ao restaurar P" + pid + " na fila I/O");
            }
        }
        return concluidos;
    }

    static int pendentes() { return Filas.tamanho(Filas.FILA_IO); }

    private static boolean processoExecutando(int i) {
        return i >= 0 && i < App.N && App.pid[i] == i && App.status[i] == App.STATUS_EXEC;
    }

    private static boolean dispositivoValido(int d) {
        return d == App.IO_DISCO || d == App.IO_FITA || d == App.IO_IMPRESSORA;
    }
}
