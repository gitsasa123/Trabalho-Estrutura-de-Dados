package arvores.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Retrato de uma árvore pronto para desenhar: os elementos (nós, marcadores...) já posicionados
 * em "unidades" (1 unidade = distância mínima entre nós vizinhos de um nível; o nível é o y).
 * A raiz fica em x = 0.
 */
final class Desenho {

    static final Desenho VAZIO = new Desenho(null, 0, 0, 0, false, 0, false);

    final NoDesenho raiz;
    final List<NoDesenho> nos;
    final Map<String, NoDesenho> porId;
    final double xMin;
    final double xMax;
    final double yMax;
    /** {@code true} se o pedido de foco foi atendido (a chave ainda existe). */
    final boolean focoAplicado;
    /** Nº de nós de verdade na subárvore em foco (só vale se {@link #focoAplicado}). */
    final long nosNoFoco;
    /** {@code true} se há marcadores ou elisões (parte da árvore ficou de fora). */
    final boolean resumido;

    Desenho(NoDesenho raiz, double xMin, double xMax, double yMax, boolean focoAplicado, long nosNoFoco,
            boolean resumido) {
        this.raiz = raiz;
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMax = yMax;
        this.focoAplicado = focoAplicado;
        this.nosNoFoco = nosNoFoco;
        this.resumido = resumido;

        List<NoDesenho> lista = new ArrayList<>();
        Map<String, NoDesenho> mapa = new HashMap<>();
        if (raiz != null) {
            Deque<NoDesenho> pilha = new ArrayDeque<>();
            pilha.push(raiz);
            while (!pilha.isEmpty()) {
                NoDesenho n = pilha.pop();
                lista.add(n);
                mapa.put(n.id, n);
                if (n.dir != null) {
                    pilha.push(n.dir);
                }
                if (n.esq != null) {
                    pilha.push(n.esq);
                }
            }
        }
        this.nos = List.copyOf(lista);
        this.porId = Map.copyOf(mapa);
    }

    boolean vazio() {
        return raiz == null;
    }

    double largura() {
        return xMax - xMin;
    }

    /** Nº de nós de verdade que aparecem no desenho (sem marcadores, elisões e fantasmas). */
    int nosVisiveis() {
        int total = 0;
        for (NoDesenho n : nos) {
            if (n.ehNo()) {
                total++;
            }
        }
        return total;
    }
}
