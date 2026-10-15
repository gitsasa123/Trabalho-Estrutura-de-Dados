package arvores.gui.visual;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;

/** Controle deslizante: trilha fina que se enche até a bolinha. */
public final class DeslizanteUI extends BasicSliderUI {

    private static final int BOLINHA = 16;

    public DeslizanteUI(JSlider slider) {
        super(slider);
    }

    public static ComponentUI createUI(JComponent c) {
        return new DeslizanteUI((JSlider) c);
    }

    @Override
    protected void installDefaults(JSlider s) {
        super.installDefaults(s);
        s.setOpaque(false);
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(BOLINHA, BOLINHA);
    }

    @Override
    public void paintFocus(Graphics g) {
        // o foco é mostrado em volta da bolinha
    }

    @Override
    public void paintTrack(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            double cy = trackRect.y + trackRect.height / 2.0;
            double fim = thumbRect.x + thumbRect.width / 2.0;
            g.setColor(Paleta.BORDA);
            g.fill(new RoundRectangle2D.Double(trackRect.x, cy - 2, trackRect.width, 4, 4, 4));
            g.setColor(slider.isEnabled() ? Paleta.PRIMARIO : Paleta.PRIMARIO_DESABILITADO);
            g.fill(new RoundRectangle2D.Double(trackRect.x, cy - 2, Math.max(0, fim - trackRect.x), 4, 4, 4));
        } finally {
            g.dispose();
        }
    }

    @Override
    public void paintThumb(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            double x = thumbRect.x + (thumbRect.width - BOLINHA) / 2.0;
            double y = thumbRect.y + (thumbRect.height - BOLINHA) / 2.0;
            if (slider.hasFocus()) {
                g.setColor(Paleta.comAlfa(Paleta.FOCO, 0.30));
                g.fill(new Ellipse2D.Double(x - 3, y - 3, BOLINHA + 6, BOLINHA + 6));
            }
            g.setColor(Paleta.comAlfa(Color.BLACK, 0.10));
            g.fill(new Ellipse2D.Double(x - 0.5, y + 0.5, BOLINHA + 1, BOLINHA + 1));
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(x, y, BOLINHA, BOLINHA));
            g.setColor(slider.isEnabled() ? Paleta.PRIMARIO : Paleta.PRIMARIO_DESABILITADO);
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(x + 1, y + 1, BOLINHA - 2, BOLINHA - 2));
        } finally {
            g.dispose();
        }
    }
}
