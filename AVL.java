package arvores;

import java.util.ArrayList;
import java.util.List;

/**
 * Árvore AVL: uma BST que se mantém balanceada.
 *
 * Regra: em TODO nó, as alturas das subárvores esquerda e direita diferem em no máximo 1
 * ("fator de balanceamento" = altura(esq) - altura(dir), sempre em {-1, 0, +1}).
 * Quando uma inserção ou remoção quebra a regra, o nó é consertado com ROTAÇÕES.
 * Resultado: altura sempre O(log n) (no máximo ~1,44 * log2(n)).
 *
 * Os métodos são recursivos porque a altura é pequena (log n): não há risco de estourar a pilha.
 */
public class AVL<K extends Comparable<K>, V> implements Arvore<K, V> {

    private static final class No<K, V> {
        K chave;
        V dados;
        No<K, V> esq;
        No<K, V> dir;
        int altura = 1; // altura do nó = nº de nós do caminho mais longo até uma folha (folha = 1)

        No(K chave, V dados) {
            this.chave = chave;
            this.dados = dados;
        }
    }

    private No<K, V> raiz;
    private int tamanho;
    private long comparacoes;
    private boolean removeu; // "bilhete" que a recursão de remover usa para avisar se achou a chave

    /** Única porta de comparação de chaves: assim nenhuma comparação escapa da contagem. */
    private int comparar(K a, K b) {
        comparacoes++;
        return a.compareTo(b);
    }

    // ------------------------------------------------------------------ altura e balanceamento

    private int alt(No<K, V> n) {
        return (n == null) ? 0 : n.altura;
    }

    private void atualizarAltura(No<K, V> n) {
        n.altura = 1 + Math.max(alt(n.esq), alt(n.dir));
    }

    /** Fator de balanceamento: positivo = pesado para a esquerda, negativo = pesado para a direita. */
    private int fator(No<K, V> n) {
        return (n == null) ? 0 : alt(n.esq) - alt(n.dir);
    }

    /**
     * Rotação à direita em torno de y (usada quando y está pesado para a ESQUERDA):
     *
     *        y                x
     *       / \              / \
     *      x   C    --->    A   y
     *     / \                  / \
     *    A   B                B   C
     *
     * A ordem das chaves é preservada (A < x < B < y < C); só a forma muda.
     */
    private No<K, V> rotacaoDireita(No<K, V> y) {
        No<K, V> x = y.esq;
        y.esq = x.dir;
        x.dir = y;
        atualizarAltura(y); // y ficou embaixo: atualiza ele primeiro
        atualizarAltura(x);
        return x; // x é a nova raiz dessa subárvore
    }

    /** Espelho da anterior: usada quando x está pesado para a DIREITA. */
    private No<K, V> rotacaoEsquerda(No<K, V> x) {
        No<K, V> y = x.dir;
        x.dir = y.esq;
        y.esq = x;
        atualizarAltura(x);
        atualizarAltura(y);
        return y;
    }

    /**
     * Atualiza a altura do nó e, se ele ficou desbalanceado (|fator| = 2), aplica a rotação certa.
     * Serve tanto depois de inserir quanto depois de remover.
     *   Caso esquerda-esquerda: rotação simples à direita.
     *   Caso esquerda-direita : rotação dupla (primeiro esquerda no filho, depois direita no nó).
     *   E os dois casos espelhados para a direita.
     */
    private No<K, V> balancear(No<K, V> n) {
        atualizarAltura(n);
        int fb = fator(n);
        if (fb > 1) {                       // pesado para a esquerda
            if (fator(n.esq) < 0) {         // o filho esquerdo pende para a direita: rotação dupla
                n.esq = rotacaoEsquerda(n.esq);
            }
            return rotacaoDireita(n);
        }
        if (fb < -1) {                      // pesado para a direita
            if (fator(n.dir) > 0) {         // o filho direito pende para a esquerda: rotação dupla
                n.dir = rotacaoDireita(n.dir);
            }
            return rotacaoEsquerda(n);
        }
        return n;                           // já estava balanceado
    }

    // ------------------------------------------------------------------ inserir

    @Override
    public void inserir(K chave, V dados) {
        raiz = inserir(raiz, chave, dados);
    }

    /** Insere na subárvore de 'no' e devolve a nova raiz dessa subárvore (já balanceada). */
    private No<K, V> inserir(No<K, V> no, K chave, V dados) {
        if (no == null) {
            tamanho++;
            return new No<>(chave, dados);
        }
        int c = comparar(chave, no.chave);
        if (c == 0) {
            no.dados = dados;   // chave repetida: substitui e não precisa rebalancear
            return no;
        }
        if (c < 0) {
            no.esq = inserir(no.esq, chave, dados);
        } else {
            no.dir = inserir(no.dir, chave, dados);
        }
        return balancear(no);   // na volta da recursão, conserta cada nó do caminho
    }

    // ------------------------------------------------------------------ buscar

    @Override
    public V buscar(K chave) {
        No<K, V> atual = raiz;
        while (atual != null) {
            int c = comparar(chave, atual.chave);
            if (c == 0) {
                return atual.dados;
            }
            atual = (c < 0) ? atual.esq : atual.dir;
        }
        return null;
    }

    // ------------------------------------------------------------------ remover

    @Override
    public boolean remover(K chave) {
        removeu = false;
        raiz = remover(raiz, chave);
        if (removeu) {
            tamanho--;
        }
        return removeu;
    }

    private No<K, V> remover(No<K, V> no, K chave) {
        if (no == null) {
            return null; // chave não existe
        }
        int c = comparar(chave, no.chave);
        if (c < 0) {
            no.esq = remover(no.esq, chave);
        } else if (c > 0) {
            no.dir = remover(no.dir, chave);
        } else {
            removeu = true;
            if (no.esq == null) {
                return no.dir;  // 0 ou 1 filho: o filho (ou null) sobe no lugar
            }
            if (no.dir == null) {
                return no.esq;
            }
            // 2 filhos: copia o sucessor (menor chave da direita) para este nó
            // e apaga o nó do sucessor lá embaixo.
            No<K, V> sucessor = no.dir;
            while (sucessor.esq != null) {
                sucessor = sucessor.esq;
            }
            no.chave = sucessor.chave;
            no.dados = sucessor.dados;
            no.dir = removerMenor(no.dir);
        }
        return balancear(no);   // REBALANCEIA subindo pelo caminho (exigência do enunciado)
    }

    /** Remove o menor nó da subárvore (o que está mais à esquerda), rebalanceando no caminho. */
    private No<K, V> removerMenor(No<K, V> no) {
        if (no.esq == null) {
            return no.dir;
        }
        no.esq = removerMenor(no.esq);
        return balancear(no);
    }

    // ------------------------------------------------------------------ altura, percursos, etc.

    @Override
    public int altura() {
        return alt(raiz); // a altura já está guardada em cada nó: custo O(1)
    }

    @Override
    public List<K> percorrer(Ordem ordem) {
        List<K> resultado = new ArrayList<>(tamanho);
        percorrer(raiz, ordem, resultado);
        return resultado;
    }

    private void percorrer(No<K, V> no, Ordem ordem, List<K> saida) {
        if (no == null) {
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
     * Confere TUDO que uma AVL precisa ter:
     *  - chaves em ordem estritamente crescente (propriedade da BST);
     *  - altura guardada em cada nó confere com a altura real;
     *  - fator de balanceamento de todo nó em {-1, 0, +1};
     *  - número de nós igual a 'tamanho'.
     * (usa compareTo direto para não alterar o contador de comparações)
     */
    @Override
    public boolean validar() {
        int[] contagem = {0};
        return validar(raiz, null, null, contagem) >= 0 && contagem[0] == tamanho;
    }

    /** Devolve a altura real da subárvore, ou -1 se alguma regra foi violada. */
    private int validar(No<K, V> n, K min, K max, int[] contagem) {
        if (n == null) {
            return 0;
        }
        contagem[0]++;
        if (min != null && n.chave.compareTo(min) <= 0) return -1; // tem que ser > min
        if (max != null && n.chave.compareTo(max) >= 0) return -1; // tem que ser < max
        int he = validar(n.esq, min, n.chave, contagem);
        int hd = validar(n.dir, n.chave, max, contagem);
        if (he < 0 || hd < 0) return -1;
        if (Math.abs(he - hd) > 1) return -1;                      // desbalanceada
        int h = 1 + Math.max(he, hd);
        if (n.altura != h) return -1;                              // altura guardada errada
        return h;
    }
}
