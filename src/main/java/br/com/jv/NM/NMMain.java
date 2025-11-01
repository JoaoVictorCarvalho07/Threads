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
