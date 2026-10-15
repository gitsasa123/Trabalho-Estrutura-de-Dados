package arvores.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.OptionalInt;

import arvores.Cor;
import arvores.Faixa;
import arvores.NoVisivel;

/**
 * Tira um retrato (Desenho) de uma árvore viva. Como uma árvore de 100 mil nós não cabe na tela,
 * o retrato mostra só os primeiros níveis (veja {@link PedidoDesenho}); cada subárvore que ficou de
 * fora vira um marcador com a contagem de nós escondidos. O caminho de uma busca é sempre mostrado
 * inteiro; se for longo demais (BST degenerada), o miolo vira uma elisão.
 *
 * <p>Tudo é iterativo ou tem recursão limitada pelo nº de níveis pedido, então funciona mesmo para
 * uma BST com 100 mil níveis.
 */
final class ConstrutorDesenho {

    private static final int MIN_NIVEIS_ELIDIDOS = 3;

    private final List<Integer> caminho;
    private final Integer chaveFantasma;
    private final int niveisExpandidos;
    private final boolean elide;
    private final int cabeca;
    private final int cauda;
    private boolean resumido;

    private ConstrutorDesenho(List<Integer> caminho, Integer chaveFantasma, int niveisExpandidos, int maxNiveis) {
        this.caminho = caminho;
        this.chaveFantasma = chaveFantasma;
        this.niveisExpandidos = niveisExpandidos;
        this.cabeca = Math.max(2, maxNiveis / 2);
        this.cauda = Math.max(2, maxNiveis - cabeca);
        this.elide = caminho.size() > maxNiveis && caminho.size() - cabeca - cauda >= MIN_NIVEIS_ELIDIDOS;
    }

    static Desenho construir(NoVisivel<Integer, Faixa> raizReal, PedidoDesenho pedido) {
        if (raizReal == null) {
            return Desenho.VAZIO;
        }
        NoVisivel<Integer, Faixa> raiz = raizReal;
        List<Integer> caminho = pedido.caminho();
        boolean focoAplicado = false;
        long nosNoFoco = 0;

        if (pedido.chaveFoco() != null) {
            NoVisivel<Integer, Faixa> foco = localizar(raizReal, pedido.chaveFoco());
            if (foco != null) {
                raiz = foco;
                focoAplicado = true;
                nosNoFoco = contar(foco);
                int i = caminho.indexOf(pedido.chaveFoco());
                caminho = (i >= 0) ? caminho.subList(i, caminho.size()) : List.of();
            }
        }
        int chaveRaiz = raiz.chave();
        if (!caminho.isEmpty() && caminho.get(0) != chaveRaiz) {
            caminho = List.of(); // caminho de outra árvore/estado: ignora
        }

        int maxNiveis = Math.max(1, pedido.maxNiveis());
        int niveis = niveisExpandidos(raiz, maxNiveis, Math.max(2, pedido.maxPorNivel()));
        ConstrutorDesenho c = new ConstrutorDesenho(caminho, pedido.chaveFantasma(), niveis, maxNiveis);

        NoDesenho raizDesenho = c.construirNo(raiz, 0, caminho.isEmpty() ? -1 : 0);
        double[] limites = LayoutArvore.posicionar(raizDesenho);
        return new Desenho(raizDesenho, limites[0], limites[1], limites[2], focoAplicado, nosNoFoco, c.resumido);
    }

    // ------------------------------------------------------------------ quantos níveis cabem

    /**
     * Nº de níveis mostrados inteiros. Para ao chegar em maxNiveis ou quando o desenho ficaria largo
     * demais: a fileira de marcadores que vem logo abaixo também tem que caber em maxPorNivel.
     */
    private static int niveisExpandidos(NoVisivel<Integer, Faixa> raiz, int maxNiveis, int maxPorNivel) {
        List<NoVisivel<Integer, Faixa>> nivel = List.of(raiz);
        int niveis = 1;
        while (niveis < maxNiveis) {
            List<NoVisivel<Integer, Faixa>> proximo = filhos(nivel);
            if (proximo.isEmpty() || proximo.size() > maxPorNivel) {
                break;
            }
            if (filhos(proximo).size() > maxPorNivel) {
                break; // mostrar este nível obrigaria uma fileira de marcadores larga demais
            }
            nivel = proximo;
            niveis++;
        }
        return niveis;
    }

    private static List<NoVisivel<Integer, Faixa>> filhos(List<NoVisivel<Integer, Faixa>> nivel) {
        List<NoVisivel<Integer, Faixa>> filhos = new ArrayList<>();
        for (NoVisivel<Integer, Faixa> n : nivel) {
            if (n.esquerdo() != null) {
                filhos.add(n.esquerdo());
            }
            if (n.direito() != null) {
                filhos.add(n.direito());
            }
        }
        return filhos;
    }

    // ------------------------------------------------------------------ construção

    private NoDesenho construirNo(NoVisivel<Integer, Faixa> n, int nivel, int indiceCaminho) {
        OptionalInt fator = n.fatorBalanceamento();
        NoDesenho d = new NoDesenho(NoDesenho.Tipo.NO, NoDesenho.idDoNo(n.chave()), n.chave(), n.dados(),
                n.cor(), fator.isPresent(), fator.orElse(0), 0);
        d.esq = filho(n, n.esquerdo(), true, nivel, indiceCaminho);
        d.dir = filho(n, n.direito(), false, nivel, indiceCaminho);
        return d;
    }

    private NoDesenho filho(NoVisivel<Integer, Faixa> pai, NoVisivel<Integer, Faixa> f, boolean esquerdo,
                            int nivel, int indiceCaminho) {
        if (f == null) {
            return fantasma(pai, esquerdo, indiceCaminho);
        }
        boolean noCaminho = indiceCaminho >= 0 && indiceCaminho + 1 < caminho.size()
                && caminho.get(indiceCaminho + 1) == f.chave().intValue();
        if (noCaminho) {
            if (elide && indiceCaminho + 1 == cabeca) {
                return elisao(pai, f, esquerdo);
            }
            return construirNo(f, nivel + 1, indiceCaminho + 1);
        }
        if (nivel + 1 < niveisExpandidos) {
            return construirNo(f, nivel + 1, -1);
        }
        return marcador(pai, f, esquerdo);
    }

    /** O lugar vazio no fim do caminho, onde a chave buscada/inserida ficaria. */
    private NoDesenho fantasma(NoVisivel<Integer, Faixa> pai, boolean esquerdo, int indiceCaminho) {
        if (chaveFantasma == null || indiceCaminho < 0 || indiceCaminho != caminho.size() - 1) {
            return null;
        }
        boolean vaiParaEsquerda = Integer.compare(chaveFantasma, pai.chave()) < 0;
        if (vaiParaEsquerda != esquerdo) {
            return null;
        }
        return new NoDesenho(NoDesenho.Tipo.FANTASMA, NoDesenho.idDoNo(chaveFantasma), chaveFantasma, null,
                Cor.NENHUMA, false, 0, 0);
    }

    private NoDesenho marcador(NoVisivel<Integer, Faixa> pai, NoVisivel<Integer, Faixa> f, boolean esquerdo) {
        resumido = true;
        return new NoDesenho(NoDesenho.Tipo.MARCADOR, "m" + pai.chave() + (esquerdo ? "E" : "D"), f.chave(),
                null, Cor.NENHUMA, false, 0, contar(f));
    }

    /**
     * Troca o miolo de um caminho longo por um único elemento "⋮". {@code primeiro} é o primeiro nó
     * omitido; o desenho continua nos últimos nós do caminho.
     */
    private NoDesenho elisao(NoVisivel<Integer, Faixa> pai, NoVisivel<Integer, Faixa> primeiro, boolean esquerdo) {
        int retomada = caminho.size() - cauda;
        NoVisivel<Integer, Faixa> atual = primeiro;
        for (int i = cabeca + 1; i <= retomada && atual != null; i++) {
            int proxima = caminho.get(i);
            if (atual.esquerdo() != null && atual.esquerdo().chave() == proxima) {
                atual = atual.esquerdo();
            } else if (atual.direito() != null && atual.direito().chave() == proxima) {
                atual = atual.direito();
            } else {
                atual = null;
            }
        }
        if (atual == null) {
            return marcador(pai, primeiro, esquerdo); // caminho não bate com a árvore: resume tudo
        }
        resumido = true;
        NoDesenho e = new NoDesenho(NoDesenho.Tipo.ELISAO, "e" + pai.chave(), pai.chave(), null, Cor.NENHUMA,
                false, 0, retomada - cabeca);
        e.dir = construirNo(atual, retomada, retomada);
        return e;
    }

    // ------------------------------------------------------------------ utilidades

    private static NoVisivel<Integer, Faixa> localizar(NoVisivel<Integer, Faixa> raiz, int chave) {
        NoVisivel<Integer, Faixa> atual = raiz;
        while (atual != null) {
            int c = Integer.compare(chave, atual.chave());
            if (c == 0) {
                return atual;
            }
            atual = (c < 0) ? atual.esquerdo() : atual.direito();
        }
        return null;
    }

    /** Conta os nós de uma subárvore sem recursão (pode ter 100 mil níveis). */
    static long contar(NoVisivel<Integer, Faixa> raiz) {
        long total = 0;
        Deque<NoVisivel<Integer, Faixa>> pilha = new ArrayDeque<>();
        pilha.push(raiz);
        while (!pilha.isEmpty()) {
            NoVisivel<Integer, Faixa> n = pilha.pop();
            total++;
            if (n.esquerdo() != null) {
                pilha.push(n.esquerdo());
            }
            if (n.direito() != null) {
                pilha.push(n.direito());
            }
        }
        return total;
    }
}
