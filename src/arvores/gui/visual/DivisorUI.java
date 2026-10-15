package arvores.gui.visual;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/** Divisória entre dois painéis: um fundo liso com uma pílula no meio, que escurece ao passar o mouse. */
public final class DivisorUI extends BasicSplitPaneUI {

    public static ComponentUI createUI(JComponent c) {
        return new DivisorUI();
    }

    @Override
    public BasicSplitPaneDivider createDefaultDivider() {
        return new Divisoria(this);
    }

    @SuppressWarnings("serial")
    private static final class Divisoria extends BasicSplitPaneDivider {

        private boolean sobre;

        Divisoria(BasicSplitPaneUI ui) {
            super(ui);
            setBorder(null);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    mudar(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    mudar(false);
                }
            });
        }

        private void mudar(boolean novo) {
            if (sobre != novo) {
                sobre = novo;
                repaint();
            }
        }

        @Override
        public void paint(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            try {
                Paleta.qualidade(g);
                g.setColor(Paleta.FUNDO);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(sobre ? Paleta.TEXTO_3 : Paleta.BORDA_FORTE);
                boolean divisorVertical = splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT;
                double comprimento = 40;
                double espessura = 4;
                double x = divisorVertical ? (getWidth() - espessura) / 2 : (getWidth() - comprimento) / 2;
                double y = divisorVertical ? (getHeight() - comprimento) / 2 : (getHeight() - espessura) / 2;
                g.fill(new RoundRectangle2D.Double(x, y, divisorVertical ? espessura : comprimento,
                        divisorVertical ? comprimento : espessura, espessura, espessura));
            } finally {
                g.dispose();
            }
        }
    }
}
