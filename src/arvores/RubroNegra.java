package arvores;

import java.util.ArrayList;
import java.util.List;

public class RubroNegra<K extends Comparable<K>, V> implements Arvore<K, V> {

    private static final boolean VERMELHO = true;
    private static final boolean PRETO = false;

    private static final class No<K, V> implements NoVisivel<K, V> {
        K chave;
        V dados;
        boolean cor = VERMELHO;   // todo nó novo entra vermelho
        No<K, V> esq;
        No<K, V> dir;
        No<K, V> pai;

        // O NIL é o único nó sem chave: para quem olha de fora, ele equivale a "sem filho".
        private NoVisivel<K, V> real(No<K, V> filho) {
            return (filho.chave == null) ? null : filho;
        }

        @Override
        public K chave() {
            return chave;
        }

        @Override
        public V dados() {
            return dados;
        }

        @Override
        public NoVisivel<K, V> esquerdo() {
            return real(esq);
        }

        @Override
        public NoVisivel<K, V> direito() {
            return real(dir);
        }

        @Override
        public Cor cor() {
            return (cor == VERMELHO) ? Cor.VERMELHO : Cor.PRETO;
        }
    }

    private final No<K, V> nil = new No<>(); // sentinela: representa todas as folhas vazias
    private No<K, V> raiz;
    private int tamanho;
    private long comparacoes;

    public RubroNegra() {
        nil.cor = PRETO;
        nil.esq = nil;
        nil.dir = nil;
        nil.pai = nil;
        raiz = nil;
    }

    private int comparar(K a, K b) {
        comparacoes++;
        return a.compareTo(b);
    }

    private void rotacaoEsquerda(No<K, V> x) {
        No<K, V> y = x.dir;
        x.dir = y.esq;
        if (y.esq != nil) {
            y.esq.pai = x;
        }
        y.pai = x.pai;
        if (x.pai == nil) {
            raiz = y;
        } else if (x == x.pai.esq) {
            x.pai.esq = y;
        } else {
            x.pai.dir = y;
        }
        y.esq = x;
        x.pai = y;
    }

    /** Espelho da anterior. */
    private void rotacaoDireita(No<K, V> y) {
        No<K, V> x = y.esq;
        y.esq = x.dir;
        if (x.dir != nil) {
            x.dir.pai = y;
        }
        x.pai = y.pai;
        if (y.pai == nil) {
            raiz = x;
        } else if (y == y.pai.esq) {
            y.pai.esq = x;
        } else {
            y.pai.dir = x;
        }
        x.dir = y;
        y.pai = x;
    }

    @Override
    public void inserir(K chave, V dados) {
        No<K, V> pai = nil;
        No<K, V> atual = raiz;
        int c = 0;
        while (atual != nil) {
            pai = atual;
            c = comparar(chave, atual.chave);
            if (c == 0) {
                atual.dados = dados;  // chave repetida: substitui
                return;
            }
            atual = (c < 0) ? atual.esq : atual.dir;
        }

        No<K, V> z = new No<>();
        z.chave = chave;
        z.dados = dados;
        z.pai = pai;
        z.esq = nil;
        z.dir = nil;
        z.cor = VERMELHO;
        if (pai == nil) {
            raiz = z;
        } else if (c < 0) {
            pai.esq = z;
        } else {
            pai.dir = z;
        }
        tamanho++;

        consertarInsercao(z);
    }

    private void consertarInsercao(No<K, V> z) {
        while (z.pai.cor == VERMELHO) {
            if (z.pai == z.pai.pai.esq) {
                No<K, V> tio = z.pai.pai.dir;
                if (tio.cor == VERMELHO) {                       // caso 1
                    z.pai.cor = PRETO;
                    tio.cor = PRETO;
                    z.pai.pai.cor = VERMELHO;
                    z = z.pai.pai;
                } else {
                    if (z == z.pai.dir) {                        // caso 2
                        z = z.pai;
                        rotacaoEsquerda(z);
                    }
                    z.pai.cor = PRETO;                           // caso 3
                    z.pai.pai.cor = VERMELHO;
                    rotacaoDireita(z.pai.pai);
                }
            } else {                                             // mesmos casos, lado espelhado
                No<K, V> tio = z.pai.pai.esq;
                if (tio.cor == VERMELHO) {
                    z.pai.cor = PRETO;
                    tio.cor = PRETO;
                    z.pai.pai.cor = VERMELHO;
                    z = z.pai.pai;
                } else {
                    if (z == z.pai.esq) {
                        z = z.pai;
                        rotacaoDireita(z);
                    }
                    z.pai.cor = PRETO;
                    z.pai.pai.cor = VERMELHO;
                    rotacaoEsquerda(z.pai.pai);
                }
            }
        }
        raiz.cor = PRETO; // regra 2
    }

    private No<K, V> encontrar(K chave) {
        No<K, V> atual = raiz;
        while (atual != nil) {
            int c = comparar(chave, atual.chave);
            if (c == 0) {
                return atual;
            }
            atual = (c < 0) ? atual.esq : atual.dir;
        }
        return nil;
    }

    @Override
    public V buscar(K chave) {
        No<K, V> n = encontrar(chave);
        return (n == nil) ? null : n.dados;
    }

    private void transplantar(No<K, V> u, No<K, V> v) {
        if (u.pai == nil) {
            raiz = v;
        } else if (u == u.pai.esq) {
            u.pai.esq = v;
        } else {
            u.pai.dir = v;
        }
        v.pai = u.pai; // vale mesmo quando v é o NIL: o conserto usa esse pai
    }

    private No<K, V> minimo(No<K, V> n) {
        while (n.esq != nil) {
            n = n.esq;
        }
        return n;
    }

    @Override
    public boolean remover(K chave) {
        No<K, V> z = encontrar(chave);
        if (z == nil) {
            return false;
        }

        No<K, V> y = z;                 // nó que realmente sai (ou é movido) da árvore
        boolean corOriginalDeY = y.cor;
        No<K, V> x;                     // nó que assume o lugar de y

        if (z.esq == nil) {             // sem filho esquerdo: o direito sobe
            x = z.dir;
            transplantar(z, z.dir);
        } else if (z.dir == nil) {      // sem filho direito: o esquerdo sobe
            x = z.esq;
            transplantar(z, z.esq);
        } else {                        // 2 filhos: o sucessor y toma o lugar de z (herdando a cor de z)
            y = minimo(z.dir);
            corOriginalDeY = y.cor;
            x = y.dir;
            if (y.pai == z) {
                x.pai = y;
            } else {
                transplantar(y, y.dir);
                y.dir = z.dir;
                y.dir.pai = y;
            }
            transplantar(z, y);
            y.esq = z.esq;
            y.esq.pai = y;
            y.cor = z.cor;
        }
        tamanho--;

        if (corOriginalDeY == PRETO) {
            consertarRemocao(x);
        }
        nil.pai = nil; // o NIL pode ter ficado com um pai temporário; limpamos
        return true;
    }

    private void consertarRemocao(No<K, V> x) {
        while (x != raiz && x.cor == PRETO) {
            if (x == x.pai.esq) {
                No<K, V> w = x.pai.dir;
                if (w.cor == VERMELHO) {                              // caso 1
                    w.cor = PRETO;
                    x.pai.cor = VERMELHO;
                    rotacaoEsquerda(x.pai);
                    w = x.pai.dir;
                }
                if (w.esq.cor == PRETO && w.dir.cor == PRETO) {       // caso 2
                    w.cor = VERMELHO;
                    x = x.pai;
                } else {
                    if (w.dir.cor == PRETO) {                         // caso 3
                        w.esq.cor = PRETO;
                        w.cor = VERMELHO;
                        rotacaoDireita(w);
                        w = x.pai.dir;
                    }
                    w.cor = x.pai.cor;                                // caso 4
                    x.pai.cor = PRETO;
                    w.dir.cor = PRETO;
                    rotacaoEsquerda(x.pai);
                    x = raiz;
                }
            } else {                                                  // mesmos casos, lado espelhado
                No<K, V> w = x.pai.esq;
                if (w.cor == VERMELHO) {
                    w.cor = PRETO;
                    x.pai.cor = VERMELHO;
                    rotacaoDireita(x.pai);
                    w = x.pai.esq;
                }
                if (w.dir.cor == PRETO && w.esq.cor == PRETO) {
                    w.cor = VERMELHO;
                    x = x.pai;
                } else {
                    if (w.esq.cor == PRETO) {
                        w.dir.cor = PRETO;
                        w.cor = VERMELHO;
                        rotacaoEsquerda(w);
                        w = x.pai.esq;
                    }
                    w.cor = x.pai.cor;
                    x.pai.cor = PRETO;
                    w.esq.cor = PRETO;
                    rotacaoDireita(x.pai);
                    x = raiz;
                }
            }
        }
        x.cor = PRETO;
    }

    @Override
    public int altura() {
        return altura(raiz);
    }

    private int altura(No<K, V> n) {
        if (n == nil) {
            return 0;
        }
        return 1 + Math.max(altura(n.esq), altura(n.dir));
    }

    @Override
    public List<K> percorrer(Ordem ordem) {
        List<K> resultado = new ArrayList<>(tamanho);
        percorrer(raiz, ordem, resultado);
        return resultado;
    }

    private void percorrer(No<K, V> no, Ordem ordem, List<K> saida) {
        if (no == nil) {
            return;
        }
        if (ordem == Ordem.PRE) saida.add(no.chave);
        percorrer(no.esq, ordem, saida);
        if (ordem == Ordem.EM) saida.add(no.chave);
        percorrer(no.dir, ordem, saida);
        if (ordem == Ordem.POS) saida.add(no.chave);
    }

    @Override
    public long contadorComparacoes() {
        return comparacoes;
    }

    @Override
    public void zerarContador() {
        comparacoes = 0;
    }

    @Override
    public int tamanho() {
        return tamanho;
    }

    @Override
    public NoVisivel<K, V> raizVisivel() {
        return (raiz == nil) ? null : raiz;
    }

    @Override
    public boolean validar() {
        if (nil.cor != PRETO || raiz.cor != PRETO) {
            return false;
        }
        if (raiz != nil && raiz.pai != nil) {
            return false;
        }
        int[] contagem = {0};
        return validar(raiz, null, null, contagem) >= 0 && contagem[0] == tamanho;
    }

    private int validar(No<K, V> n, K min, K max, int[] contagem) {
        if (n == nil) {
            return 1; // o NIL é preto e conta como 1
        }
        contagem[0]++;
        if (min != null && n.chave.compareTo(min) <= 0) return -1;
        if (max != null && n.chave.compareTo(max) >= 0) return -1;
        if (n.esq != nil && n.esq.pai != n) return -1;
        if (n.dir != nil && n.dir.pai != n) return -1;
        if (n.cor == VERMELHO && (n.esq.cor == VERMELHO || n.dir.cor == VERMELHO)) return -1;
        int he = validar(n.esq, min, n.chave, contagem);
        int hd = validar(n.dir, n.chave, max, contagem);
        if (he < 0 || hd < 0 || he != hd) return -1;
        return he + (n.cor == PRETO ? 1 : 0);
    }
}
