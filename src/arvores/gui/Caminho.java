package arvores.gui;

import java.util.ArrayList;
import java.util.List;

import arvores.Faixa;
import arvores.NoVisivel;

/**
 * Caminho que uma busca percorre na árvore: as chaves dos nós visitados, da raiz até o último.
 * Calculado "por fora" (com {@code compareTo} direto), então não mexe no contador de comparações.
 *
 * @param chaves    chaves visitadas, na ordem
 * @param encontrou {@code true} se o último nó visitado tem a chave procurada
 */
record Caminho(List<Integer> chaves, boolean encontrou) {

    static final Caminho VAZIO = new Caminho(List.of(), false);

    static Caminho ate(NoVisivel<Integer, Faixa> raiz, int chave) {
        List<Integer> visitadas = new ArrayList<>();
        NoVisivel<Integer, Faixa> atual = raiz;
        while (atual != null) {
            int chaveAtual = atual.chave();
            visitadas.add(chaveAtual);
            int c = Integer.compare(chave, chaveAtual);
            if (c == 0) {
                return new Caminho(visitadas, true);
            }
            atual = (c < 0) ? atual.esquerdo() : atual.direito();
        }
        return new Caminho(visitadas, false);
    }

    boolean vazio() {
        return chaves.isEmpty();
    }

    int ultima() {
        return chaves.get(chaves.size() - 1);
    }
}
