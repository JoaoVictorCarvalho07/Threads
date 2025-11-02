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

            //nessa operação estamos acumulando a variavel
            acum+=i;
        }

        // if apenas para o
        if (acum == 42) System.out.print("");
        uso = acum;
        CHECK.addAndGet(acum);
    }
}
