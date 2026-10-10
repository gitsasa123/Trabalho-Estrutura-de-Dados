package arvores;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Testes que valem para QUALQUER implementação de Arvore (BST, AVL, Rubro-Negra).
 * Compara a árvore com o TreeMap do Java (que serve de "gabarito") em milhares de
 * operações aleatórias e chama validar() depois de cada uma.
 *
 * Para testar as outras árvores, adicione-as no main().
 */
public final class TesteArvore {

    private static int verificacoes = 0;

    private TesteArvore() {
    }

    private static void confere(boolean condicao, String mensagem) {
        verificacoes++;
        if (!condicao) {
            throw new AssertionError("FALHOU: " + mensagem);
        }
    }

    public static void main(String[] args) {
        testarTudo("BST", BST::new, 0);
        testarTudo("AVL", AVL::new, 1.45);          // altura <= 1,45 * log2(n + 2)
        testarTudo("Rubro-Negra", RubroNegra::new, 2.0);   // altura <= 2 * log2(n + 2)
        System.out.println("\nOK: " + verificacoes + " verificações passaram.");
    }

    /**
     * @param fatorAltura 0 = árvore sem balanceamento (não checa altura);
     *                    senão, a altura precisa ser <= fatorAltura * log2(n + 2) + 1
     */
    static void testarTudo(String nome, Supplier<Arvore<Integer, String>> fabrica, double fatorAltura) {
        System.out.println("== " + nome + " ==");
        testeBasico(nome, fabrica);
        testeRepetidaEContador(nome, fabrica);
        for (long seed : new long[] {12345L, 99L, 2026L}) {
            testeAleatorio(nome, fabrica, seed, 20000, 2000);
        }
        testeAleatorio(nome, fabrica, 777L, 20000, 50);   // poucas chaves: muita remoção/reinserção
        testeAleatorio(nome, fabrica, 31L, 20000, 8);     // árvores minúsculas: casos de borda
        testeOrdenado(nome, fabrica, 20000, fatorAltura);
        testeOrdemInversa(nome, fabrica, 20000, fatorAltura);
    }

    private static void testeBasico(String nome, Supplier<Arvore<Integer, String>> fabrica) {
        Arvore<Integer, String> a = fabrica.get();
        confere(a.tamanho() == 0 && a.altura() == 0, nome + ": árvore vazia");
        confere(a.buscar(1) == null, nome + ": buscar em árvore vazia");
        confere(!a.remover(1), nome + ": remover em árvore vazia");
        confere(a.percorrer(Ordem.EM).isEmpty(), nome + ": percurso vazio");

        for (int k : new int[] {50, 30, 70, 20, 40, 60, 80}) {
            a.inserir(k, "v" + k);
        }
        confere(a.tamanho() == 7, nome + ": tamanho 7");
        confere("v60".equals(a.buscar(60)), nome + ": buscar 60");
        confere(a.buscar(65) == null, nome + ": buscar inexistente");
        confere(a.validar(), nome + ": validar");
        confere(a.percorrer(Ordem.EM).equals(List.of(20, 30, 40, 50, 60, 70, 80)), nome + ": em-ordem");
    }

    private static void testeRepetidaEContador(String nome, Supplier<Arvore<Integer, String>> fabrica) {
        Arvore<Integer, String> a = fabrica.get();
        a.inserir(10, "a");
        confere(a.contadorComparacoes() == 0, nome + ": inserir na raiz vazia não compara");
        a.inserir(5, "b");
        confere(a.contadorComparacoes() >= 1, nome + ": segundo nó compara pelo menos 1 vez");
        a.inserir(10, "novo");
        confere(a.tamanho() == 2, nome + ": chave repetida não aumenta o tamanho");
        confere("novo".equals(a.buscar(10)), nome + ": chave repetida substitui os dados");
        a.zerarContador();
        confere(a.contadorComparacoes() == 0, nome + ": zerarContador");
        a.buscar(5);
        confere(a.contadorComparacoes() >= 1, nome + ": buscar conta comparações");
    }

    private static void testeAleatorio(String nome, Supplier<Arvore<Integer, String>> fabrica,
                                       long seed, int operacoes, int faixaDeChaves) {
        Random rnd = new Random(seed);
        Arvore<Integer, String> a = fabrica.get();
        TreeMap<Integer, String> gabarito = new TreeMap<>();

        for (int i = 0; i < operacoes; i++) {
            int chave = rnd.nextInt(faixaDeChaves);
            int op = rnd.nextInt(10);
            if (op < 5) {
                String v = "v" + i;
                a.inserir(chave, v);
                gabarito.put(chave, v);
            } else if (op < 8) {
                boolean existia = gabarito.remove(chave) != null;
                confere(a.remover(chave) == existia, nome + ": remover(" + chave + ") retorno, op " + i);
            } else {
                confere(java.util.Objects.equals(a.buscar(chave), gabarito.get(chave)),
                        nome + ": buscar(" + chave + "), op " + i);
            }
            confere(a.tamanho() == gabarito.size(), nome + ": tamanho, op " + i);
            confere(a.validar(), nome + ": validar() depois da op " + i);

            if (i % 200 == 0) {
                conferePercursos(nome, a, gabarito, i);
            }
        }
        conferePercursos(nome, a, gabarito, operacoes);
        System.out.printf("  aleatório (seed %d, %d chaves): ok, altura final %d com %d nós%n",
                seed, faixaDeChaves, a.altura(), a.tamanho());
    }

    /** Em-ordem igual ao gabarito; pré e pós-ordem consistentes com a MESMA árvore. */
    private static void conferePercursos(String nome, Arvore<Integer, String> a,
                                         TreeMap<Integer, String> gabarito, int op) {
        List<Integer> em = a.percorrer(Ordem.EM);
        confere(em.equals(new ArrayList<>(gabarito.keySet())), nome + ": em-ordem, op " + op);

        List<Integer> pre = a.percorrer(Ordem.PRE);
        List<Integer> pos = a.percorrer(Ordem.POS);
        confere(pre.size() == em.size() && pos.size() == em.size(), nome + ": tamanhos dos percursos, op " + op);

        // Pré-ordem + em-ordem determinam uma única árvore; a pós-ordem dela
        // precisa ser a pós-ordem que a implementação devolveu.
        Map<Integer, Integer> posicaoEm = new HashMap<>();
        for (int i = 0; i < em.size(); i++) {
            posicaoEm.put(em.get(i), i);
        }
        List<Integer> esperadoPos = new ArrayList<>();
        reconstruirPos(pre, new int[] {0}, 0, em.size() - 1, posicaoEm, esperadoPos);
        confere(esperadoPos.equals(pos), nome + ": pós-ordem consistente com pré+em, op " + op);
    }

    private static void reconstruirPos(List<Integer> pre, int[] proximo, int ini, int fim,
                                       Map<Integer, Integer> posicaoEm, List<Integer> saida) {
        if (ini > fim) {
            return;
        }
        int raiz = pre.get(proximo[0]++);
        int meio = posicaoEm.get(raiz);
        reconstruirPos(pre, proximo, ini, meio - 1, posicaoEm, saida);
        reconstruirPos(pre, proximo, meio + 1, fim, posicaoEm, saida);
        saida.add(raiz);
    }

    /** Chaves em ordem crescente: pior caso da BST. Mostra altura e comparações. */
    private static void testeOrdenado(String nome, Supplier<Arvore<Integer, String>> fabrica, int n,
                                      double fatorAltura) {
        Arvore<Integer, String> a = fabrica.get();
        for (int i = 1; i <= n; i++) {
            a.inserir(i, "v" + i);
        }
        confere(a.tamanho() == n && a.validar(), nome + ": inserção ordenada");
        confere(a.buscar(n) != null && a.buscar(1) != null, nome + ": busca após inserção ordenada");
        conferirAlturaLogaritmica(nome, a, n, fatorAltura);
        System.out.printf("  ordenado (%d chaves): altura %d, comparações até aqui %d%n",
                n, a.altura(), a.contadorComparacoes());
        for (int i = 1; i <= n; i += 2) {
            confere(a.remover(i), nome + ": remover " + i);
            if (i % 1001 == 1) {
                confere(a.validar(), nome + ": validar durante a remoção ordenada, i=" + i);
            }
        }
        confere(a.tamanho() == n / 2 && a.validar(), nome + ": remoção de metade das chaves");
        conferirAlturaLogaritmica(nome, a, n / 2, fatorAltura);
    }

    /** Chaves em ordem decrescente: o outro pior caso da BST (todas vão para a esquerda). */
    private static void testeOrdemInversa(String nome, Supplier<Arvore<Integer, String>> fabrica, int n,
                                          double fatorAltura) {
        Arvore<Integer, String> a = fabrica.get();
        for (int i = n; i >= 1; i--) {
            a.inserir(i, "v" + i);
        }
        confere(a.tamanho() == n && a.validar(), nome + ": inserção em ordem decrescente");
        conferirAlturaLogaritmica(nome, a, n, fatorAltura);
        for (int i = n; i >= 1; i -= 3) {
            confere(a.remover(i), nome + ": remover (decrescente) " + i);
        }
        confere(a.validar(), nome + ": validar após remoção decrescente");
    }

    private static void conferirAlturaLogaritmica(String nome, Arvore<Integer, String> a, int n, double fator) {
        if (fator <= 0) {
            return; // BST: sem garantia de altura
        }
        double limite = fator * (Math.log(n + 2) / Math.log(2)) + 1;
        confere(a.altura() <= limite,
                nome + ": altura " + a.altura() + " deveria ser <= " + limite + " para n=" + n);
    }
}
