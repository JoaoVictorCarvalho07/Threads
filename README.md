# Comparação de Desempenho entre Modelos de Threads **N:M** e **1:1** (Java)

> •Trabalho prático – Performance em Sistemas Ciberfísicos  
> •Autores: Joao Victor Carvalho de Freitas ,Matheus Henrique Heinzen,Edmund Soares de Souza,Vinicius Lima Teider  
> • Grupo: Pratica 2  
> •Java: _`21`_  
> • SO/CPU: windows 11; 10 Cores/12 Threads

---

## Objetivo

Implementar e comparar dois modelos de execução concorrente:

- **1:1** – cada _thread de usuário_ mapeia **1 thread do SO**;
- **N:M** – **N** tarefas (threads de usuário) são **multiplexadas** em **M** threads reais do SO usando **pool fixo**.

A comparação é feita pelo **tempo total de execução** ao processar a **mesma tarefa** para diferentes valores de **N** (ex.: 10, 100, 500, 1000, 10000).

---

## Ideia da Tarefa por Thread (CPU‑bound)

Cada tarefa executa um laço determinístico de `iteracoes` vezes, fazendo operações aritméticas/bit a bit. Para impedir que o JIT elimine o trabalho (_dead‑code elimination_), o resultado final é escrito em uma variável `volatile`.

```java
package br.com.jv;

import java.util.concurrent.atomic.AtomicLong;

public class TarefaBase implements Runnable {
    private final int numIteracoes;
    public static volatile long  uso;
    public static final AtomicLong CHECK = new AtomicLong();

    public TarefaBase(int numIteracoes) {
        this.numIteracoes = numIteracoes;
    }

    @Override
    public void run() {

        long acum = 0;
        for (int i = 0; i < numIteracoes; i++) {

            //nessa operação estamos acumulando a variavel acum
            acum+=i;
        }

        // usos apenas para o jit nao descartar o loop
        if (acum == 42) System.out.print("");
        uso = acum;
        CHECK.addAndGet(acum);
    }
}
```

> **Por que isso?** O **JIT** (Just‑In‑Time compiler) pode remover loops cujo resultado não é usado. Escrever em um `volatile` ou num acumulador global impede essa otimização e garante que a carga **realmente rode**.

---

## Implementações

### Modelo **N:M** (pool fixo)

```java

package br.com.jv.NM;

import br.com.jv.TarefaBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class NMMain {
    public static void main(String[] args) throws InterruptedException {
        int N = args.length > 0 ? Integer.parseInt(args[0]) : 1000; // 1000 tarefas
        int M = args.length > 1 ? Integer.parseInt(args[1]) : 10; // 10 threads

        int iteracoes = args.length > 2 ? Integer.parseInt(args[2]) : 5_000_000; // 5 milhoes de iteracoes

        //aqui o programa executara apenas o numero fixo de threads = 10
        ExecutorService executor = Executors.newFixedThreadPool(M);

        List<Future<?>> futures = new ArrayList<>(N);


        long t0 = System.nanoTime();

        //aqui ele esta adicionando as tarefas ao executor
        //fila de tarefas a serem executadas com as threads disponivel (8)
        for (int i = 0; i < N; i++) {
            futures.add(executor.submit(new TarefaBase(iteracoes)));
        }

        for (Future<?> f : futures) {
            try {
                //aqui ele esta bloqueando a thread atual ate a tarefa terminar
                //pra nao executar mais de uma tarefa na mesma thread .
                f.get();
            } catch (ExecutionException e) {
                e.getCause().printStackTrace();
            }
        }



        long t1 = System.nanoTime();

        executor.shutdown();

        long tfinal = TimeUnit.NANOSECONDS.toMillis(t1 - t0);

        System.out.printf("N:M | N=%d tarefas, M=%d threads | work=%d | tempo=%d ms%n", N, M, iteracoes, tfinal);
    }
}

```

### Modelo **1:1** (cada tarefa = 1 thread do SO)

```java
package br.com.jv.oto;

import br.com.jv.TarefaBase;

import java.util.concurrent.TimeUnit;

public class OtoMain {
    public static void main(String[] args) throws InterruptedException {
        int N = args.length > 0 ? Integer.parseInt(args[0]) : 100; //numero de threads
        int iteracoes = args.length > 1 ? Integer.parseInt(args[1]) : 5000000;

        Thread[] threads = new Thread[N];

        long t0 = System.nanoTime();
        for (int i = 0; i < N; i++) {
            threads[i] = new Thread(new TarefaBase(iteracoes),"oto-" + i);
            threads[i].start();

        }

        for (int i = 0; i < N; i++) {
            threads[i].join();
        }

        long t1 = System.nanoTime();
        long tfinal = TimeUnit.NANOSECONDS.toMillis(t1 - t0);

        System.out.printf("1:1 | N=%d threads | iteracoes/tarefa=%d | tempo=%d ms%n",
                N, iteracoes, tfinal);

    }
}

```

---

## ▶️ Como Compilar e Executar (PowerShell no Windows)

### 1) Abrir a pasta do projeto

### 2) Compilar

```powershell
mkdir -Force target\classes | Out-Null
javac -d target\classes `
  src\main\java\br\com\jv\TarefaBase.java `
  src\main\java\br\com\jv\NM\NMMain.java `
  src\main\java\br\com\jv\oto\OtoMain.java
```

### 3) Executar

```powershell
# N:M — argumentos: N M iteracoes
java -cp target\classes br.com.jv.NM.NMMain 1000 10 5000000


# 1:1 — argumentos: N iteracoes
java -cp target\classes br.com.jv.oto.OtoMain 1000 5000000
```

> **Parâmetros**
>
> - `N`: nº de tarefas (N:M) ou nº de threads reais (1:1)
> - `M`: tamanho do pool (apenas no N:M)
> - `iteracoes`: numero de loops da tarefa da tarefa

---

## Metodologia de Medição

1. **Warm‑up:** executado 1 vez fora da cronometria para aquecer o JIT.
2. **Cronometria:** medição de **antes do submit/start** até **após get/join**.
3. **Repetições:** executado **5 vezes** cada configuração; usando **média** (e desvio padrão).
4. **Sanity check:** `N=1` em **ambos** deve dar valores muito próximos (M=1).
5. **Ambiente:** java 21 windows 11/; 10 Cores/12 Threads; rodados em ambiente normal de uso(navegador e ide abertos);
   fonte de energia conectada.

---

## Resultados

### N:M — `iteracoes = 5_000_000`

| N (tarefas) | M (threads) | Tempo médio (ms) |
| ----------: | ----------: | ---------------: |
|        1000 |          10 |              208 |
|        1000 |         100 |              207 |
|        1000 |         500 |              213 |
|        1000 |        1000 |              272 |
|       10000 |          10 |             1789 |
|       10000 |         100 |             1524 |
|       10000 |         500 |             1654 |
|       10000 |        1000 |             1613 |

### 1:1 — `iteracoes = 5_000_000`

| N (threads reais) | Tempo médio (ms) |
| ----------------: | ---------------: |
|              1000 |              288 |
|             10000 |             3171 |

---

## Prints das execuções

### Prints das execuções (exemplo)
---
Figura 1 — NM: N=1000, M=10

![NM — N=1000, M=10](prints%20execução%20do%20programa/NM_1000N_10M.png)

---
Figura 2 — NM: N=1000, M=100

![NM — N=1000, M=100](prints%20execução%20do%20programa/NM_1000N_100M.png)

---
Figura 3 — NM: N=1000, M=500

![NM — N=1000, M=500](prints%20execução%20do%20programa/NM_1000N_500M.png)

---
Figura 4 — NM: N=1000, M=1000

![NM — N=1000, M=1000](prints%20execução%20do%20programa/NM_1000N_1000M.png)

---
Figura 5 — 1:1 (OtO): N=1000

![OtO — N=1000](prints%20execução%20do%20programa/OtO_1000M.png)

---
Figura 6 — 1:1 (OtO): N=10000

![OtO — N=10000](prints%20execução%20do%20programa/OtO_10000M.png)

---


## Análise

> “Com tarefa **CPU‑bound** fixa (5.000.000 iterações por tarefa), o tempo total cresceu quase linearmente com **N**. No modelo **N:M**, variar **M** acima do número de **núcleos lógicos** não trouxe ganhos expressivos; ao contrário, valores excessivos adicionaram **overhead** de agendamento. Para **N** elevado, o modelo **1:1** tende a sofrer por conta do **overhead de criação/gerência** de muitas threads nativas (pilhas maiores, **context switching**), enquanto **N:M** mantém throughput mais estável ao limitar a concorrência real. Assim, para cargas CPU‑bound, **N:M com M≈núcleos lógicos** se mostrou mais eficiente e previsível. Em cargas IO‑bound, o comportamento pode inverter, pois threads bloqueadas não consomem CPU.”

---

## Comandos Rápidos (resumo)

```powershell
# Compilar
javac -d target\classes `
  src\main\java\br\com\jv\TarefaBase.java `
  src\main\java\br\com\jv\NM\NMMain.java `
  src\main\java\br\com\jv\oto\OtoMain.java

# Rodar N:M (N M iteracoes)
java -cp target\classes br.com.jv.NM.NMMain 1000 10 5000000

# Rodar 1:1 (N iteracoes)
java -cp target\classes br.com.jv.oto.OtoMain 1000 5000000
```
