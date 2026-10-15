package arvores.gui;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import arvores.Arvore;
import arvores.CarregadorFaixas;
import arvores.Faixa;
import arvores.TipoArvore;

/**
 * Insere as faixas em três árvores novas, uma thread por árvore (a BST com chaves em ordem
 * crescente é lentíssima e não deve fazer as outras esperarem). Só devolve as árvores no final:
 * se for cancelado, as árvores que já estavam na tela ficam como estavam.
 */
final class CargaDataset {

    /** Quem acompanha a carga. Os métodos são chamados de threads de trabalho, não da thread de eventos. */
    interface Ouvinte {

        void andamento(TipoArvore tipo, double fracao);

        boolean cancelado();
    }

    record Resultado(EnumMap<TipoArvore, Arvore<Integer, Faixa>> arvores, EnumMap<TipoArvore, Long> nanos,
                     EnumMap<TipoArvore, Long> comparacoes) {
    }

    private static final int MASCARA_ANDAMENTO = 1023;

    private CargaDataset() {
    }

    /** @throws CancellationException se o ouvinte pedir o cancelamento */
    static Resultado executar(List<CarregadorFaixas.Registro> registros, Ouvinte ouvinte)
            throws InterruptedException, ExecutionException {
        TipoArvore[] tipos = TipoArvore.values();
        ExecutorService pool = Executors.newFixedThreadPool(tipos.length);
        try {
            EnumMap<TipoArvore, Future<long[]>> tarefas = new EnumMap<>(TipoArvore.class);
            EnumMap<TipoArvore, Arvore<Integer, Faixa>> arvores = new EnumMap<>(TipoArvore.class);
            for (TipoArvore tipo : tipos) {
                Arvore<Integer, Faixa> arvore = tipo.criar();
                arvores.put(tipo, arvore);
                tarefas.put(tipo, pool.submit(() -> inserirTudo(tipo, arvore, registros, ouvinte)));
            }
            EnumMap<TipoArvore, Long> nanos = new EnumMap<>(TipoArvore.class);
            EnumMap<TipoArvore, Long> comparacoes = new EnumMap<>(TipoArvore.class);
            for (TipoArvore tipo : tipos) {
                try {
                    long[] r = tarefas.get(tipo).get();
                    nanos.put(tipo, r[0]);
                    comparacoes.put(tipo, r[1]);
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof CancellationException c) {
                        throw c;
                    }
                    throw e;
                }
            }
            return new Resultado(arvores, nanos, comparacoes);
        } finally {
            pool.shutdownNow();
        }
    }

    /** Devolve {nanossegundos, comparações}. */
    private static long[] inserirTudo(TipoArvore tipo, Arvore<Integer, Faixa> arvore,
                                      List<CarregadorFaixas.Registro> registros, Ouvinte ouvinte) {
        long inicio = System.nanoTime();
        int feitas = 0;
        for (CarregadorFaixas.Registro r : registros) {
            arvore.inserir(r.chave(), r.dados());
            if ((++feitas & MASCARA_ANDAMENTO) == 0) {
                if (ouvinte.cancelado()) {
                    throw new CancellationException("Carga cancelada");
                }
                ouvinte.andamento(tipo, (double) feitas / registros.size());
            }
        }
        long nanos = System.nanoTime() - inicio;
        long comparacoes = arvore.contadorComparacoes();
        arvore.zerarContador();
        ouvinte.andamento(tipo, 1.0);
        return new long[] {nanos, comparacoes};
    }
}
