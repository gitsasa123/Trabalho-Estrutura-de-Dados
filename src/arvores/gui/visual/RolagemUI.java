package arvores.gui.visual;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Barras de rolagem finas, sem setas: só uma pílula cinza que engrossa ao passar o mouse. */
public final class RolagemUI extends BasicScrollBarUI {

    private static final Color NORMAL = new Color(0xC9CED8);
    private static final Color SOBRE = new Color(0xA7AEBD);

    public static ComponentUI createUI(JComponent c) {
        return new RolagemUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        scrollbar.setOpaque(false);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return botaoInvisivel();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return botaoInvisivel();
    }

    private static JButton botaoInvisivel() {
        JButton b = new JButton() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(0, 0);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(0, 0);
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(0, 0);
            }
        };
        b.setFocusable(false);
        b.setRequestFocusEnabled(false);
        b.setBorder(null);
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // a trilha é transparente
    }

    @Override
    protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
        if (r.width <= 0 || r.height <= 0 || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            boolean vertical = scrollbar.getOrientation() == JScrollBar.VERTICAL;
            boolean destacada = isDragging || isThumbRollover();
            double espessura = destacada ? 8 : 6;
            double x = vertical ? r.x + (r.width - espessura) / 2 : r.x + 2;
            double y = vertical ? r.y + 2 : r.y + (r.height - espessura) / 2;
            double w = vertical ? espessura : r.width - 4;
            double h = vertical ? r.height - 4 : espessura;
            g.setColor(isDragging ? Paleta.TEXTO_3 : destacada ? SOBRE : NORMAL);
            g.fill(new RoundRectangle2D.Double(x, y, w, h, Math.min(w, h), Math.min(w, h)));
        } finally {
            g.dispose();
        }
    }
}
