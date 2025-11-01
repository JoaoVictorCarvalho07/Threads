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
            threads[i] = new Thread(new TarefaBase(iteracoes));
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
