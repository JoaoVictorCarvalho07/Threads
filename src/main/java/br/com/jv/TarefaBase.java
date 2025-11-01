package br.com.jv;

public class TarefaBase implements Runnable {
    private final int numIteracoes;
    public static volatile long  uso;

    public TarefaBase(int numIteracoes) {
        this.numIteracoes = numIteracoes;
    }

    @Override
    public void run() {

        long acum = 0;
        for (int i = 0; i < numIteracoes; i++) {

            //nessa operação estamos acumulando a variavel
            // i*31L multiplica por 31long (forcando uma operacao com long mais custoso)
            // ^ faz o XOR bit a bit , diferentes vira 1 e iguais vira 0
            // acum >>> 3 faz com que preencha os bits a esquerda com 0 nao preservando bit de sinal
            // é uma operação simples mas custosa para base de teste de performace.
            acum += (i * 31L) ^ (acum >>> 3);
        }

        // if apenas para o
        if (acum == 124435) System.out.print("");
        uso = acum;

    }
}
