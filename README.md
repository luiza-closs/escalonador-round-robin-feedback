# Simulador de escalonamento de processos

Projeto acadêmico de Sistemas Operacionais, desenvolvido em Java procedural. O código deve usar funções e estruturas de dados simples, sem modelar o problema com hierarquias de objetos.

## Regras já definidas pelo grupo

- Existem filas de prioridade alta e baixa, além de uma fila de processos aguardando I/O.
- O escalonador seleciona a fila alta antes da baixa.
- O quantum e os eventos de I/O seguem as definições finais do enunciado.
- Ao concluir o I/O, disco encaminha o processo à fila baixa; fita e impressora encaminham à fila alta.
- Registrar eventos com o tempo simulado e produzir métricas finais de espera, turnaround, preempções e ociosidade da CPU.

Confiram os valores exatos de quantum, faixas de CPU/I/O, dispositivos, critério de geração e formato de saída no enunciado antes de fechar as implementações. O link compartilhado não estava acessível durante a preparação desta base.

## Estrutura

```text
Java/
├── .env.example       # modelo de configuração
├── .env               # configuração local (não versionada)
├── .gitignore
├── README.md
└── src/
    ├── App.java       # ponto de entrada e integração
    └── Configuracao.java # leitura da configuração comum
```

Cada frente deve implementar sua parte em arquivos separados dentro de `src/`. Os nomes são sugestões; alinhem assinaturas e contratos entre as pessoas antes de integrar.

| Frente | Entrega | Arquivos sugeridos |
|---|---|---|
| 1. PCB e gerador | dados do processo, geração aleatória e rajadas | `Processo.java`, `GeradorProcessos.java` |
| 2. Filas | filas alta, baixa e de I/O | `Filas.java` |
| 3. Núcleo | laço do escalonador, quantum, preempção e ociosidade | `Escalonador.java` |
| 4. I/O | bloqueio, duração e retorno às filas | `EntradaSaida.java` |
| 5. Logs e métricas | eventos e resumo final | `Relatorio.java` |
| 6. Integração | documentação, PDF e apresentação | `README.md`, `relatorio/`, `apresentacao/` |

### Contrato de integração sugerido

- Representar o tempo simulado com inteiros (ticks), salvo orientação diferente do enunciado.
- Centralizar valores compartilhados em `Configuracao.java`; evitar números mágicos espalhados.
- Cada módulo deve expor funções públicas estáticas pequenas e documentar entradas, saídas e quem é responsável por alterar o estado do processo.
- Combinem um único formato de evento de log, por exemplo: `[t=003] P1 sofreu preempção -> fila BAIXA`.
- Não adicionem dependências externas sem necessidade; o projeto inicial compila com o JDK.

## Configuração

Copie `.env.example` para `.env` e ajuste os valores conforme o enunciado. `Configuracao.java` lê `.env` no diretório de execução; variáveis de ambiente do sistema têm precedência. Valores ausentes ou inválidos usam os padrões documentados no código.

## Compilar e executar

Com JDK instalado, a partir desta pasta:

```powershell
javac -d bin src\*.java
java -cp bin App
```

No VS Code, abra a pasta `Java`. A configuração em `.vscode/settings.json` já aponta `src` como diretório fonte e `bin` como saída.

## Git

O `.env` local e arquivos compilados estão ignorados pelo Git. Versione `.env.example` para compartilhar os nomes das configurações, nunca valores locais sensíveis.
