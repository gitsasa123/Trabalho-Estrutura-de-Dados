package arvores;

import java.util.ArrayList;
import java.util.List;

/**
 * Árvore Rubro-Negra (também chamada Vermelho-Preto), seguindo o livro do Cormen (CLRS, cap. 13).
 *
 * É uma BST em que cada nó tem uma cor e valem 5 regras:
 *   1. Todo nó é vermelho ou preto.
 *   2. A raiz é preta.
 *   3. As folhas vazias (NIL) são pretas.
 *   4. Um nó vermelho só tem filhos pretos (nunca dois vermelhos seguidos).
 *   5. Todos os caminhos de um nó até suas folhas NIL têm o mesmo número de nós pretos.
 * Dessas regras sai que o caminho mais longo é no máximo 2x o mais curto: altura <= 2*log2(n+1).
 * É menos rigorosa que a AVL, então costuma fazer menos rotações em inserção e remoção.
 *
 * Detalhes de implementação (iguais ao Cormen):
 *  - em vez de 'null', usamos UM nó sentinela NIL (preto) compartilhado por todas as folhas;
 *  - cada nó guarda o PAI, porque o conserto sobe a árvore.
 */
public class RubroNegra<K extends Comparable<K>, V> implements Arvore<K, V> {

    private static final boolean VERMELHO = true;
    private static final boolean PRETO = false;

    private static final class No<K, V> {
        K chave;
        V dados;
        boolean cor = VERMELHO;   // todo nó novo entra vermelho
        No<K, V> esq;
        No<K, V> dir;
        No<K, V> pai;
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

    // ------------------------------------------------------------------ rotações

    /**
     * Rotação à esquerda em torno de x (o filho direito y sobe, x desce para a esquerda de y):
     *
     *      x                  y
     *     / \                / \
     *    A   y     --->     x   C
     *       / \            / \
     *      B   C          A   B
     */
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

    // ------------------------------------------------------------------ inserir

    @Override
    public void inserir(K chave, V dados) {
        // 1) desce como numa BST comum procurando o lugar
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

        // 2) pendura o nó novo (vermelho) no lugar encontrado
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

        // 3) conserta a regra 4 (dois vermelhos seguidos), se ela foi quebrada
        consertarInsercao(z);
    }

    /**
     * Enquanto o pai de z é vermelho (violação), olhamos o TIO de z:
     *  - Caso 1: tio vermelho  -> recolore pai, tio e avô; o problema sobe para o avô.
     *  - Caso 2: tio preto e z é filho "de dentro" (zigue-zague) -> rotação para virar o caso 3.
     *  - Caso 3: tio preto e z é filho "de fora" (linha reta) -> recolore e faz UMA rotação; acabou.
     */
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

    // ------------------------------------------------------------------ buscar

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

    // ------------------------------------------------------------------ remover

    /** Faz o pai de u apontar para v no lugar de u (a subárvore de u sai da árvore). */
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

        // Se o nó que saiu era PRETO, sumiu um nó preto de alguns caminhos (regra 5): conserta.
        // Se era vermelho, nada foi violado.
        if (corOriginalDeY == PRETO) {
            consertarRemocao(x);
        }
        nil.pai = nil; // o NIL pode ter ficado com um pai temporário; limpamos
        return true;
    }

    /**
     * x carrega um "preto extra" (duplo preto). Olhamos o IRMÃO w de x:
     *  - Caso 1: w vermelho -> rotação + recolorir, transforma em um dos casos 2, 3 ou 4.
     *  - Caso 2: w preto com os dois filhos pretos -> w vira vermelho e o preto extra sobe para o pai.
     *  - Caso 3: w preto, filho "de dentro" vermelho e "de fora" preto -> rotação em w, vira o caso 4.
     *  - Caso 4: w preto com filho "de fora" vermelho -> recolore e rotação no pai; o preto extra some. Acabou.
     */
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

    // ------------------------------------------------------------------ altura, percursos, etc.

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

    // ------------------------------------------------------------------ validação (usada nos testes)

    /**
     * Confere as regras da Rubro-Negra e a estrutura:
     *  - raiz preta e NIL preto (regras 2 e 3);
     *  - nenhum nó vermelho com filho vermelho (regra 4);
     *  - mesma altura-preta em todos os caminhos (regra 5);
     *  - chaves em ordem estritamente crescente;
     *  - ponteiros para o pai consistentes;
     *  - número de nós igual a 'tamanho'.
     */
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

    /** Devolve a altura-preta da subárvore, ou -1 se alguma regra foi violada. */
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
