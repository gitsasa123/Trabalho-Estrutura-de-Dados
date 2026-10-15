package arvores;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class AVL<K extends Comparable<K>, V> implements Arvore<K, V> {

    private static final class No<K, V> implements NoVisivel<K, V> {
        K chave;
        V dados;
        No<K, V> esq;
        No<K, V> dir;
        int altura = 1; // altura do nó = nº de nós do caminho mais longo até uma folha (folha = 1)

        No(K chave, V dados) {
            this.chave = chave;
            this.dados = dados;
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
            return esq;
        }

        @Override
        public NoVisivel<K, V> direito() {
            return dir;
        }

        @Override
        public OptionalInt fatorBalanceamento() {
            int he = (esq == null) ? 0 : esq.altura;
            int hd = (dir == null) ? 0 : dir.altura;
            return OptionalInt.of(he - hd);
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

    private No<K, V> removerMenor(No<K, V> no) {
        if (no.esq == null) {
            return no.dir;
        }
        no.esq = removerMenor(no.esq);
        return balancear(no);
    }

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

    @Override
    public NoVisivel<K, V> raizVisivel() {
        return raiz;
    }

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
