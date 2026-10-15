package arvores.gui.visual;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

/** Barra de progresso fina e arredondada; a indeterminada é uma pílula que vai e volta. */
public final class ProgressoUI extends BasicProgressBarUI {

    private static final int ESPESSURA = 6;

    public static ComponentUI createUI(JComponent c) {
        return new ProgressoUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        progressBar.setOpaque(false);
        progressBar.setBorder(null);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return progressBar.getOrientation() == SwingConstants.HORIZONTAL
                ? new Dimension(160, ESPESSURA + 2) : new Dimension(ESPESSURA + 2, 160);
    }

    @Override
    protected int getBoxLength(int availableLength, int otherDimension) {
        return (int) Math.round(availableLength * 0.3);
    }

    @Override
    protected void paintDeterminate(Graphics g0, JComponent c) {
        Graphics2D g = preparar(g0);
        try {
            boolean horizontal = progressBar.getOrientation() == SwingConstants.HORIZONTAL;
            int w = progressBar.getWidth();
            int h = progressBar.getHeight();
            desenharTrilha(g, w, h, horizontal);
            double fracao = Math.max(0, Math.min(1, progressBar.getPercentComplete()));
            if (fracao > 0) {
                g.setColor(Paleta.PRIMARIO);
                if (horizontal) {
                    g.fill(new RoundRectangle2D.Double(0, (h - ESPESSURA) / 2.0, Math.max(ESPESSURA, w * fracao), ESPESSURA,
                            ESPESSURA, ESPESSURA));
                } else {
                    double altura = Math.max(ESPESSURA, h * fracao);
                    g.fill(new RoundRectangle2D.Double((w - ESPESSURA) / 2.0, h - altura, ESPESSURA, altura,
                            ESPESSURA, ESPESSURA));
                }
            }
        } finally {
            g.dispose();
        }
    }

    @Override
    protected void paintIndeterminate(Graphics g0, JComponent c) {
        Graphics2D g = preparar(g0);
        try {
            boolean horizontal = progressBar.getOrientation() == SwingConstants.HORIZONTAL;
            int w = progressBar.getWidth();
            int h = progressBar.getHeight();
            desenharTrilha(g, w, h, horizontal);
            Rectangle caixa = getBox(null);
            if (caixa != null) {
                g.setColor(Paleta.PRIMARIO);
                if (horizontal) {
                    g.fill(new RoundRectangle2D.Double(caixa.x, (h - ESPESSURA) / 2.0, caixa.width, ESPESSURA,
                            ESPESSURA, ESPESSURA));
                } else {
                    g.fill(new RoundRectangle2D.Double((w - ESPESSURA) / 2.0, caixa.y, ESPESSURA, caixa.height,
                            ESPESSURA, ESPESSURA));
                }
            }
        } finally {
            g.dispose();
        }
    }

    private static Graphics2D preparar(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Paleta.qualidade(g);
        return g;
    }

    private static void desenharTrilha(Graphics2D g, int w, int h, boolean horizontal) {
        g.setColor(Paleta.BORDA);
        if (horizontal) {
            g.fill(new RoundRectangle2D.Double(0, (h - ESPESSURA) / 2.0, w, ESPESSURA, ESPESSURA, ESPESSURA));
        } else {
            g.fill(new RoundRectangle2D.Double((w - ESPESSURA) / 2.0, 0, ESPESSURA, h, ESPESSURA, ESPESSURA));
        }
    }
}
