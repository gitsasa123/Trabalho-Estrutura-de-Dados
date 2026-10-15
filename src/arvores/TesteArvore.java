package arvores;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Supplier;

public final class TesteArvore {

    private static PrintStream saida = System.out;
    private static long verificacoes = 0;

    private TesteArvore() {
    }

    private static void confere(boolean condicao, String mensagem) {
        verificacoes++;
        if (!condicao) {
            throw new AssertionError("FALHOU: " + mensagem);
        }
    }

    public static void main(String[] args) {
        executar(System.out);
    }

    /**
     * Roda todos os testes, escrevendo o andamento em {@code destino}.
     *
     * @return nº de verificações que passaram
     * @throws AssertionError na primeira verificação que falhar
     */
    public static synchronized long executar(PrintStream destino) {
        saida = destino;
        verificacoes = 0;
        testarTudo("BST", BST::new, 0);
        testarTudo("AVL", AVL::new, 1.45);          // altura <= 1,45 * log2(n + 2)
        testarTudo("Rubro-Negra", RubroNegra::new, 2.0);   // altura <= 2 * log2(n + 2)
        saida.println("\nOK: " + verificacoes + " verificações passaram.");
        return verificacoes;
    }

    static void testarTudo(String nome, Supplier<Arvore<Integer, String>> fabrica, double fatorAltura) {
        saida.println("== " + nome + " ==");
        testeBasico(nome, fabrica);
        testeRepetidaEContador(nome, fabrica);
        for (long seed : new long[] {12345L, 99L, 2026L}) {
            testeAleatorio(nome, fabrica, seed, 20000, 2000);
        }
        testeAleatorio(nome, fabrica, 777L, 20000, 50);   // poucas chaves: muita remoção/reinserção
        testeAleatorio(nome, fabrica, 31L, 20000, 8);     // árvores minúsculas: casos de borda
        testeOrdenado(nome, fabrica, 20000, fatorAltura);
        testeOrdemInversa(nome, fabrica, 20000, fatorAltura);
        testeVisao(nome, fabrica);
        testeVisaoEmCadeia(nome, fabrica, 20000);
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
        saida.printf("  aleatório (seed %d, %d chaves): ok, altura final %d com %d nós%n",
                seed, faixaDeChaves, a.altura(), a.tamanho());
    }

    private static void conferePercursos(String nome, Arvore<Integer, String> a,
                                         TreeMap<Integer, String> gabarito, int op) {
        List<Integer> em = a.percorrer(Ordem.EM);
        confere(em.equals(new ArrayList<>(gabarito.keySet())), nome + ": em-ordem, op " + op);

        List<Integer> pre = a.percorrer(Ordem.PRE);
        List<Integer> pos = a.percorrer(Ordem.POS);
        confere(pre.size() == em.size() && pos.size() == em.size(), nome + ": tamanhos dos percursos, op " + op);

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

    private static void testeOrdenado(String nome, Supplier<Arvore<Integer, String>> fabrica, int n,
                                      double fatorAltura) {
        Arvore<Integer, String> a = fabrica.get();
        for (int i = 1; i <= n; i++) {
            a.inserir(i, "v" + i);
        }
        confere(a.tamanho() == n && a.validar(), nome + ": inserção ordenada");
        confere(a.buscar(n) != null && a.buscar(1) != null, nome + ": busca após inserção ordenada");
        conferirAlturaLogaritmica(nome, a, n, fatorAltura);
        saida.printf("  ordenado (%d chaves): altura %d, comparações até aqui %d%n",
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

    /** A visão somente leitura (raizVisivel) tem que ser idêntica à árvore: mesmas chaves, dados e formas. */
    private static void testeVisao(String nome, Supplier<Arvore<Integer, String>> fabrica) {
        Arvore<Integer, String> a = fabrica.get();
        confere(a.raizVisivel() == null, nome + ": árvore vazia não tem raiz visível");

        Random rnd = new Random(4242);
        TreeMap<Integer, String> gabarito = new TreeMap<>();
        for (int i = 0; i < 3000; i++) {
            int chave = rnd.nextInt(400);
            if (rnd.nextInt(10) < 6) {
                a.inserir(chave, "v" + i);
                gabarito.put(chave, "v" + i);
            } else {
                a.remover(chave);
                gabarito.remove(chave);
            }
            if (i % 25 == 0) {
                conferirVisao(nome, a, gabarito, i);
            }
        }
        conferirVisao(nome, a, gabarito, 3000);

        for (Integer chave : new ArrayList<>(gabarito.keySet())) {
            a.remover(chave);
        }
        confere(a.raizVisivel() == null, nome + ": árvore esvaziada não tem raiz visível");
    }

    private static void conferirVisao(String nome, Arvore<Integer, String> a,
                                      TreeMap<Integer, String> gabarito, int op) {
        NoVisivel<Integer, String> raiz = a.raizVisivel();
        if (raiz == null) {
            confere(gabarito.isEmpty() && a.tamanho() == 0, nome + ": raiz visível nula só se vazia, op " + op);
            return;
        }
        boolean usaCor = raiz.cor() != Cor.NENHUMA;
        boolean usaFator = raiz.fatorBalanceamento().isPresent();
        confere(!usaCor || raiz.cor() == Cor.PRETO, nome + ": raiz tem que ser preta, op " + op);

        List<Integer> pre = new ArrayList<>();
        List<Integer> em = new ArrayList<>();
        List<Integer> pos = new ArrayList<>();
        int[] medidas = percorrerVisao(nome, raiz, usaCor, usaFator, gabarito, pre, em, pos);
        confere(medidas[0] == a.altura(), nome + ": altura da visão, op " + op);
        confere(pre.equals(a.percorrer(Ordem.PRE)), nome + ": pré-ordem da visão, op " + op);
        confere(em.equals(a.percorrer(Ordem.EM)), nome + ": em-ordem da visão, op " + op);
        confere(pos.equals(a.percorrer(Ordem.POS)), nome + ": pós-ordem da visão, op " + op);
        confere(em.size() == a.tamanho(), nome + ": nº de nós da visão, op " + op);
    }

    /** Percorre a visão (recursivo: as árvores deste teste são rasas) e confere cada nó. */
    private static int[] percorrerVisao(String nome, NoVisivel<Integer, String> no, boolean usaCor,
                                        boolean usaFator, Map<Integer, String> gabarito,
                                        List<Integer> pre, List<Integer> em, List<Integer> pos) {
        if (no == null) {
            return new int[] {0, 1};   // altura 0; o NIL é preto e conta 1 na altura negra
        }
        confere(no.chave() != null, nome + ": nó visível sem chave (o NIL não pode aparecer)");
        confere(Objects.equals(no.dados(), gabarito.get(no.chave())), nome + ": dados do nó " + no.chave());
        confere((no.cor() != Cor.NENHUMA) == usaCor, nome + ": cor deve existir em todos os nós ou em nenhum");
        confere(no.fatorBalanceamento().isPresent() == usaFator,
                nome + ": fator deve existir em todos os nós ou em nenhum");

        pre.add(no.chave());
        int[] esq = percorrerVisao(nome, no.esquerdo(), usaCor, usaFator, gabarito, pre, em, pos);
        em.add(no.chave());
        int[] dir = percorrerVisao(nome, no.direito(), usaCor, usaFator, gabarito, pre, em, pos);
        pos.add(no.chave());

        int altura = 1 + Math.max(esq[0], dir[0]);
        int alturaNegra = esq[1];
        if (usaCor) {
            confere(esq[1] == dir[1], nome + ": alturas negras diferentes no nó " + no.chave());
            if (no.cor() == Cor.VERMELHO) {
                confere(corOuPreta(no.esquerdo()) && corOuPreta(no.direito()),
                        nome + ": nó vermelho " + no.chave() + " com filho vermelho");
            } else {
                alturaNegra++;
            }
        }
        if (usaFator) {
            int fator = no.fatorBalanceamento().getAsInt();
            confere(fator == esq[0] - dir[0], nome + ": fator do nó " + no.chave() + " diferente da altura real");
            confere(Math.abs(fator) <= 1, nome + ": nó " + no.chave() + " desbalanceado");
        }
        return new int[] {altura, alturaNegra};
    }

    private static boolean corOuPreta(NoVisivel<Integer, String> filho) {
        return filho == null || filho.cor() == Cor.PRETO;
    }

    /** Em cadeia (a BST vira uma lista de n nós) a visão tem que continuar fiel, sem recursão. */
    private static void testeVisaoEmCadeia(String nome, Supplier<Arvore<Integer, String>> fabrica, int n) {
        Arvore<Integer, String> a = fabrica.get();
        for (int i = 1; i <= n; i++) {
            a.inserir(i, "v" + i);
        }
        int nos = 0;
        int maiorNivel = 0;
        Deque<NoVisivel<Integer, String>> pilha = new ArrayDeque<>();
        Deque<Integer> niveis = new ArrayDeque<>();
        pilha.push(a.raizVisivel());
        niveis.push(1);
        while (!pilha.isEmpty()) {
            NoVisivel<Integer, String> no = pilha.pop();
            int nivel = niveis.pop();
            nos++;
            maiorNivel = Math.max(maiorNivel, nivel);
            if (no.esquerdo() != null) {
                pilha.push(no.esquerdo());
                niveis.push(nivel + 1);
            }
            if (no.direito() != null) {
                pilha.push(no.direito());
                niveis.push(nivel + 1);
            }
        }
        confere(nos == n, nome + ": nº de nós da visão em cadeia");
        confere(maiorNivel == a.altura(), nome + ": altura da visão em cadeia");
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
