package arvores.gui.visual;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Painel branco de cantos arredondados com um contorno fino, que agrupa uma parte da tela.
 * Os filhos que encostam nos cantos de cima e de baixo devem ser transparentes.
 */
@SuppressWarnings("serial")
public class Cartao extends JPanel {

    private static final int RAIO = 12;

    private final Color contorno;
    private boolean transparente;

    @SuppressWarnings("this-escape")
    public Cartao(LayoutManager layout) {
        this(layout, Paleta.BORDA);
    }

    @SuppressWarnings("this-escape")
    public Cartao(LayoutManager layout, Color contorno) {
        super(layout);
        this.contorno = contorno;
        setOpaque(false);
        setBackground(Paleta.CARTAO);
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    }

    /** Folga entre o contorno e o conteúdo. */
    public Cartao comMargem(int cima, int esquerda, int baixo, int direita) {
        setBorder(BorderFactory.createEmptyBorder(cima + 1, esquerda + 1, baixo + 1, direita + 1));
        return this;
    }

    /** Sem fundo nem contorno: só agrupa os filhos (para quando um cartão precisa se "apagar"). */
    public Cartao transparente() {
        transparente = true;
        repaint();
        return this;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        if (transparente) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1,
                    RAIO * 2, RAIO * 2);
            g.setColor(getBackground());
            g.fill(forma);
        } finally {
            g.dispose();
        }
    }

    @Override
    protected void paintChildren(Graphics g0) {
        if (transparente) {
            super.paintChildren(g0);
            return;
        }
        RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1,
                RAIO * 2, RAIO * 2);
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.clip(forma);
            super.paintChildren(g);
        } finally {
            g.dispose();
        }
        // o contorno vai por cima dos filhos: esconde o serrilhado do recorte nos cantos
        Graphics2D contornoG = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(contornoG);
            contornoG.setColor(contorno);
            contornoG.draw(forma);
        } finally {
            contornoG.dispose();
        }
    }
}
