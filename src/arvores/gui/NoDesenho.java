package arvores.gui;

import arvores.Cor;
import arvores.Faixa;

/**
 * Um elemento do desenho de uma árvore: pode ser um nó de verdade, um marcador que resume uma
 * subárvore escondida, uma elisão (trecho longo de um caminho omitido) ou um nó "fantasma"
 * (o lugar onde uma chave seria inserida).
 *
 * <p>É uma cópia: não guarda referências aos nós vivos da árvore, então continua válido depois
 * de novas inserções e remoções. A posição ({@link #x}, {@link #y}) é preenchida por
 * {@link LayoutArvore}.
 */
final class NoDesenho {

    enum Tipo {
        /** Nó de verdade da árvore. */
        NO,
        /** Triângulo que resume uma subárvore que não coube no desenho. */
        MARCADOR,
        /** "⋮" no meio de um caminho muito longo. */
        ELISAO,
        /** Lugar vazio onde a chave buscada/inserida ficaria. */
        FANTASMA
    }

    final Tipo tipo;
    /** Identifica o elemento entre um desenho e o seguinte (usado para animar a transição). */
    final String id;
    /** Chave do nó; no marcador, a chave da raiz da subárvore escondida. */
    final int chave;
    final Faixa dados;
    final Cor cor;
    final boolean temFator;
    final int fator;
    /** Marcador: nº de nós escondidos. Elisão: nº de níveis omitidos. */
    final long ocultos;

    NoDesenho esq;
    NoDesenho dir;

    // preenchidos pelo layout
    double x;
    double y;
    /** Deslocamento em relação ao pai (uso interno do layout). */
    double deslocamento;

    NoDesenho(Tipo tipo, String id, int chave, Faixa dados, Cor cor, boolean temFator, int fator, long ocultos) {
        this.tipo = tipo;
        this.id = id;
        this.chave = chave;
        this.dados = dados;
        this.cor = cor;
        this.temFator = temFator;
        this.fator = fator;
        this.ocultos = ocultos;
    }

    static String idDoNo(int chave) {
        return "k" + chave;
    }

    boolean ehNo() {
        return tipo == Tipo.NO;
    }

    @Override
    public String toString() {
        return tipo + "(" + id + ")";
    }
}
