import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Árvore binária de busca genérica: a ordenação é definida por um Comparator. */
public class ArvoreBinaria<T> {

    private static class No<T> {
        T valor;
        No<T> esquerda, direita;
        No(T valor) { this.valor = valor; }
    }

    private No<T> raiz;
    private final Comparator<T> comparador;
    private int tamanho;

    public ArvoreBinaria(Comparator<T> comparador) {
        this.comparador = comparador;
    }

    public int tamanho() { return tamanho; }

    /** Inserção iterativa (evita StackOverflow em dados ordenados). Ignora duplicatas. */
    public void inserir(T valor) {
        No<T> novo = new No<>(valor);
        if (raiz == null) { raiz = novo; tamanho++; return; }

        No<T> atual = raiz;
        while (true) {
            int cmp = comparador.compare(valor, atual.valor);
            if (cmp == 0) return;
            if (cmp < 0) {
                if (atual.esquerda == null) { atual.esquerda = novo; break; }
                atual = atual.esquerda;
            } else {
                if (atual.direita == null) { atual.direita = novo; break; }
                atual = atual.direita;
            }
        }
        tamanho++;
    }

    public void carregarDataset(List<T> dataset) {
        for (T item : dataset) inserir(item);
    }

    /** Busca por um "modelo" do elemento (basta preencher o campo usado no comparador). */
    public T buscar(T modelo) {
        No<T> atual = raiz;
        while (atual != null) {
            int cmp = comparador.compare(modelo, atual.valor);
            if (cmp == 0) return atual.valor;
            atual = (cmp < 0) ? atual.esquerda : atual.direita;
        }
        return null;
    }

    /** Percurso em ordem (esquerda, raiz, direita) -> lista ordenada. */
    public List<T> emOrdem() {
        List<T> saida = new ArrayList<>();
        emOrdemRec(raiz, saida);
        return saida;
    }

    private void emOrdemRec(No<T> no, List<T> saida) {
        if (no == null) return;
        emOrdemRec(no.esquerda, saida);
        saida.add(no.valor);
        emOrdemRec(no.direita, saida);
    }

    public int altura() { return alturaRec(raiz); }

    private int alturaRec(No<T> no) {
        if (no == null) return 0;
        return 1 + Math.max(alturaRec(no.esquerda), alturaRec(no.direita));
    }

    /** Desenha a árvore de lado (raiz à esquerda), limitada a uma profundidade máxima. */
    public void imprimir(int profundidadeMax) {
        imprimirRec(raiz, "", true, 0, profundidadeMax);
    }

    private void imprimirRec(No<T> no, String prefixo, boolean ultimo, int nivel, int max) {
        if (no == null) return;
        System.out.println(prefixo + (ultimo ? "└── " : "├── ") + no.valor);
        if (nivel >= max) return;
        String novoPrefixo = prefixo + (ultimo ? "    " : "│   ");
        boolean temEsq = no.esquerda != null, temDir = no.direita != null;
        if (temEsq) imprimirRec(no.esquerda, novoPrefixo, !temDir, nivel + 1, max);
        if (temDir) imprimirRec(no.direita, novoPrefixo, true, nivel + 1, max);
    }
}
