package arvores.gui;

import java.awt.Color;
import java.awt.Font;

import arvores.TipoArvore;
import arvores.gui.visual.Paleta;

/**
 * Cores e fontes da interface. As superfícies e os textos vêm da {@link Paleta} (o visual do programa);
 * as cores das árvores são as mesmas dos gráficos de scripts/gerar_graficos.py.
 */
final class Tema {

    // superfícies
    static final Color FUNDO_DESENHO = new Color(0xFBFBFD);
    static final Color PONTOS = Paleta.PONTOS;
    static final Color FUNDO_PAINEL = Paleta.FUNDO;
    static final Color LINHA = Paleta.BORDA;
    static final Color LINHA_FORTE = Paleta.BORDA_FORTE;

    // texto
    static final Color TEXTO = Paleta.TEXTO;
    static final Color TEXTO_2 = Paleta.TEXTO_2;
    static final Color TEXTO_3 = Paleta.TEXTO_3;

    // desenho das árvores
    static final Color ARESTA = new Color(0xA7ADBB);
    static final Color NO_VERMELHO = new Color(0xD32F2F);
    static final Color NO_PRETO = new Color(0x212121);

    // estados (cada um também tem uma forma diferente: não dependem só da cor)
    static final Color VISITA = new Color(0xF2A900);
    static final Color ACHADO = new Color(0x2E9E5B);
    static final Color NAO_ACHADO = new Color(0xD64545);
    static final Color INSERIDO = new Color(0x6C4DE6);
    static final Color PERCURSO = new Color(0x0E8FA3);
    static final Color SELECAO = Paleta.TEXTO;

    private static final Color BST = new Color(0x2A78D6);
    private static final Color AVL = new Color(0xEB6834);
    private static final Color RUBRO_NEGRA = new Color(0x1BAF7A);

    private Tema() {
    }

    static Color corDaArvore(TipoArvore tipo) {
        return switch (tipo) {
            case BST -> BST;
            case AVL -> AVL;
            case RUBRO_NEGRA -> RUBRO_NEGRA;
        };
    }

    /** Mistura {@code cor} com branco: 0 = a própria cor, 1 = branco. */
    static Color clarear(Color cor, double quanto) {
        return misturar(cor, Color.WHITE, quanto);
    }

    static Color escurecer(Color cor, double quanto) {
        return misturar(cor, Color.BLACK, quanto);
    }

    static Color misturar(Color a, Color b, double t) {
        return Paleta.misturar(a, b, t);
    }

    static Color comAlfa(Color cor, double alfa) {
        return Paleta.comAlfa(cor, alfa);
    }

    static Font fonte(float tamanho, int estilo) {
        return Paleta.fonte(tamanho, estilo);
    }
}
