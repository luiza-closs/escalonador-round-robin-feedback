# Trabalho Final de Sistemas Operacionais

Simulador de escalonamento de processos usando Round Robin com Feedback, com duas filas de prioridade (alta e baixa) e uma fila de I/O.

## Grupo

- Brenda Jaiany de Oliveira
- Fernanda Luísa Ambros
- Larissa Quadros Silva Costa
- Luiza Closs
- Luiza Manuela Dapper
- Matheus Crispin

## Linguagem utilizada

Java. Como o trabalho não podia ter orientação a objetos, não existe classe de processo. O PCB é um conjunto de vetores no `App.java`, e cada arquivo só junta as funções estáticas de uma parte do trabalho.

| Arquivo | O que tem |
|---|---|
| `App.java` | constantes, PCB, gerador de números aleatórios, criação dos processos e o `main` |
| `Filas.java` | filas alta, baixa e de I/O (fila circular em vetor) |
| `Escalonador.java` | laço principal, quantum, preempção, plano de I/O e linha do tempo da CPU |
| `EntradaSaida.java` | bloqueio por I/O e retorno para a fila certa |
| `LogsMetricas.java` | mensagem de cada evento e o resumo final |
| `Configuracao.java` | lê o quantum e a semente do arquivo `.env` |

## Premissas do escalonador

- Quantum: 2 unidades para quem sai da fila alta e 4 para quem sai da fila baixa.
- Número máximo de processos: 8 (`MAX_PROCESSOS` no `App.java`).
- Tempos de CPU: sorteados entre 8 e 25 unidades por processo.
- Tempos de I/O: disco de 3 a 7, fita de 4 a 9 e impressora de 5 a 10 unidades.
- Critério de geração dos processos: os 8 são criados em t=0 e entram na fila alta. O P0 é o pai de todos (PPID -1, porque os PIDs começam em 0) e os outros têm PPID 0.
- Quando um processo pede I/O: antes da simulação começar, cada processo recebe um plano com 0 a 3 pedidos de I/O, cada um com o dispositivo e o ponto da CPU em que acontece. Esse plano é impresso no começo da saída.
- Feedback entre as filas:
  - acabou o quantum e o processo não terminou: vai para a fila baixa;
  - voltou do disco: fila baixa;
  - voltou da fita ou da impressora: fila alta.
- Fila de I/O: uma só para os três dispositivos. Os pedidos contam o tempo em paralelo, então dois processos podem estar no disco ao mesmo tempo.
- Fim da simulação: quando os 8 processos finalizam.
- Semente aleatória: 42. Com a mesma semente a simulação sai sempre igual.

O quantum e a semente podem ser trocados no `.env` (explicado abaixo). O resto fica no código.

## Como executar o projeto

Precisa do JDK 11 ou mais novo. Na pasta do projeto:

```bash
# Linux / macOS
javac -encoding UTF-8 -d bin src/*.java
java -cp bin App
```

```powershell
# Windows (PowerShell)
javac -encoding UTF-8 -d bin src\*.java
java -cp bin App
```

Para mudar o quantum ou a semente, copie o `.env.example` para `.env` e edite os valores. Dá também para mudar só numa execução, por exemplo `SEMENTE=1 java -cp bin App` no Linux. Com a semente 1 aparece um pouco de CPU ociosa, o que não acontece com a 42.

Não fizemos a parte bônus do Docker.

## O que aparece na saída

A saída vem em blocos:

1. `[config]`: quantum de cada fila, número de processos e semente.
2. Criação dos processos e estado inicial: a tabela do PCB e o conteúdo das três filas.
3. Plano de I/O: `Impressora @2` quer dizer que o processo pede impressora depois de usar 2 unidades de CPU.
4. Simulação: uma linha por evento, sempre com o tempo na frente.
   - `CPU inicia P2 (fila ALTA, CPU restante=13)`: o processo ganhou a CPU;
   - `P2 sofreu preempção -> FILA BAIXA`: acabou o quantum;
   - `P1 solicitou I/O Disco por 3 unidade(s)`: saiu da CPU e ficou bloqueado;
   - `P1 retornou de Disco -> FILA BAIXA`: terminou o I/O e voltou para a fila;
   - `P0 finalizado`: o processo terminou toda a CPU que precisava;
   - `CPU ociosa por N unidade(s)`: ninguém pronto, todo mundo esperando I/O.
5. Estado final: tabela do PCB e filas depois que tudo terminou.
6. Linha do tempo da CPU: quem ocupou a CPU em cada intervalo, por exemplo `[005 - 006) P3`.
7. Resumo: chegada, fim, espera e turnaround de cada processo, e depois tempo total, preempções, pedidos de I/O por dispositivo, espera média, turnaround médio e CPU ociosa.

Começo da simulação com a semente 42:

```text
[t=000] CPU inicia P0 (fila ALTA, CPU restante=9)
[t=002] P0 solicitou I/O Impressora por 5 unidade(s)
[t=002] CPU inicia P1 (fila ALTA, CPU restante=13)
[t=003] P1 solicitou I/O Disco por 3 unidade(s)
[t=003] CPU inicia P2 (fila ALTA, CPU restante=13)
[t=005] P2 sofreu preempção -> FILA BAIXA
[t=005] CPU inicia P3 (fila ALTA, CPU restante=17)
[t=006] P1 retornou de Disco -> FILA BAIXA
[t=006] P3 solicitou I/O Disco por 6 unidade(s)
[t=006] CPU inicia P4 (fila ALTA, CPU restante=22)
[t=007] P0 retornou de Impressora -> FILA ALTA
```

E o final:

```text
[fim] Processos finalizados: 8/8 | tempo total: 130
Preempções: 27
Solicitações de I/O: Disco=6 | Fita=3 | Impressora=6
Espera média: 77.63 | turnaround médio: 106.00
CPU ociosa: 0 unidade(s) (0.00%)
```

## Limitações conhecidas

- Todos os processos chegam em t=0. Nenhum processo chega no meio da simulação.
- Para mudar a quantidade de processos é preciso alterar `MAX_PROCESSOS` e compilar de novo.
- Não tem fila separada por dispositivo (era uma melhoria opcional). Os pedidos de I/O correm em paralelo.
- Quem volta do I/O para a fila alta não tira da CPU um processo da fila baixa que já está rodando: espera a fatia dele acabar.
- A espera conta só o tempo parado nas filas de pronto. O tempo bloqueado em I/O fica de fora.
- A saída é longa (umas 240 linhas) porque mostra todos os eventos.
- Sem Docker.
