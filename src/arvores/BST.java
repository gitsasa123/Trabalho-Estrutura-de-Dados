package arvores;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Árvore binária de busca (sem balanceamento).
 *
 * Todos os métodos são ITERATIVOS de propósito: inserindo as chaves em ordem
 * crescente a árvore vira uma lista (altura = n) e uma versão recursiva
 * estouraria a pilha (StackOverflowError) com dezenas de milhares de nós.
 */
public class BST<K extends Comparable<K>, V> implements Arvore<K, V> {

    private static final class No<K, V> {
        K chave;
        V dados;
        No<K, V> esq;
        No<K, V> dir;

        No(K chave, V dados) {
            this.chave = chave;
            this.dados = dados;
        }
    }

    private No<K, V> raiz;
    private int tamanho;
    private long comparacoes;

    /** Única porta de comparação de chaves: assim nenhuma comparação escapa da contagem. */
    private int comparar(K a, K b) {
        comparacoes++;
        return a.compareTo(b);
    }

    @Override
    public void inserir(K chave, V dados) {
        if (raiz == null) {
            raiz = new No<>(chave, dados);
            tamanho = 1;
            return;
        }
        No<K, V> atual = raiz;
        while (true) {
            int c = comparar(chave, atual.chave);
            if (c == 0) {
                atual.dados = dados; // chave repetida: substitui
                return;
            }
            if (c < 0) {
                if (atual.esq == null) {
                    atual.esq = new No<>(chave, dados);
                    tamanho++;
                    return;
                }
                atual = atual.esq;
            } else {
                if (atual.dir == null) {
                    atual.dir = new No<>(chave, dados);
                    tamanho++;
                    return;
                }
                atual = atual.dir;
            }
        }
    }

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
        // 1) localizar o nó e o pai dele
        No<K, V> pai = null;
        No<K, V> atual = raiz;
        boolean ehFilhoEsq = false;
        while (atual != null) {
            int c = comparar(chave, atual.chave);
            if (c == 0) {
                break;
            }
            pai = atual;
            if (c < 0) {
                atual = atual.esq;
                ehFilhoEsq = true;
            } else {
                atual = atual.dir;
                ehFilhoEsq = false;
            }
        }
        if (atual == null) {
            return false;
        }

        if (atual.esq != null && atual.dir != null) {
            // 2 filhos: troca pelo sucessor (menor chave da subárvore direita)
            // e remove o nó do sucessor, que tem no máximo um filho (direito).
            No<K, V> paiSucessor = atual;
            No<K, V> sucessor = atual.dir;
            while (sucessor.esq != null) {
                paiSucessor = sucessor;
                sucessor = sucessor.esq;
            }
            atual.chave = sucessor.chave;
            atual.dados = sucessor.dados;
            if (paiSucessor == atual) {
                paiSucessor.dir = sucessor.dir;
            } else {
                paiSucessor.esq = sucessor.dir;
            }
        } else {
            // 0 ou 1 filho: o filho (ou null) ocupa o lugar do nó
            No<K, V> filho = (atual.esq != null) ? atual.esq : atual.dir;
            if (pai == null) {
                raiz = filho;
            } else if (ehFilhoEsq) {
                pai.esq = filho;
            } else {
                pai.dir = filho;
            }
        }
        tamanho--;
        return true;
    }

    @Override
    public int altura() {
        if (raiz == null) {
            return 0;
        }
        // percurso por níveis: conta quantos níveis existem
        Deque<No<K, V>> fila = new ArrayDeque<>();
        fila.add(raiz);
        int h = 0;
        while (!fila.isEmpty()) {
            h++;
            int nivel = fila.size();
            for (int i = 0; i < nivel; i++) {
                No<K, V> n = fila.poll();
                if (n.esq != null) fila.add(n.esq);
                if (n.dir != null) fila.add(n.dir);
            }
        }
        return h;
    }

    @Override
    public List<K> percorrer(Ordem ordem) {
        List<K> resultado = new ArrayList<>(tamanho);
        if (raiz == null) {
            return resultado;
        }
        Deque<No<K, V>> pilha = new ArrayDeque<>();
        switch (ordem) {
            case PRE -> {
                pilha.push(raiz);
                while (!pilha.isEmpty()) {
                    No<K, V> n = pilha.pop();
                    resultado.add(n.chave);
                    if (n.dir != null) pilha.push(n.dir); // dir entra primeiro para sair depois
                    if (n.esq != null) pilha.push(n.esq);
                }
            }
            case EM -> {
                No<K, V> atual = raiz;
                while (atual != null || !pilha.isEmpty()) {
                    while (atual != null) {
                        pilha.push(atual);
                        atual = atual.esq;
                    }
                    atual = pilha.pop();
                    resultado.add(atual.chave);
                    atual = atual.dir;
                }
            }
            case POS -> {
                // gera "raiz, direita, esquerda" e inverte no final -> "esquerda, direita, raiz"
                pilha.push(raiz);
                while (!pilha.isEmpty()) {
                    No<K, V> n = pilha.pop();
                    resultado.add(n.chave);
                    if (n.esq != null) pilha.push(n.esq);
                    if (n.dir != null) pilha.push(n.dir);
                }
                Collections.reverse(resultado);
            }
        }
        return resultado;
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
    public boolean validar() {
        // em-ordem deve ser estritamente crescente e ter exatamente 'tamanho' chaves
        // (usa compareTo direto para não mexer no contador de comparações)
        List<K> chaves = percorrer(Ordem.EM);
        if (chaves.size() != tamanho) {
            return false;
        }
        for (int i = 1; i < chaves.size(); i++) {
            if (chaves.get(i - 1).compareTo(chaves.get(i)) >= 0) {
                return false;
            }
        }
        return true;
    }
}
