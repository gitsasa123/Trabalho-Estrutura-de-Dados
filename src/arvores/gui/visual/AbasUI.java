package arvores.gui.visual;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * Abas "de sublinhado": só o texto, e a aba escolhida ganha uma barra escura embaixo. A faixa das abas
 * é branca e termina numa linha fina. Veja {@link Estilo#compactas} para a versão menor.
 */
public final class AbasUI extends BasicTabbedPaneUI {

    private static final Insets ABA = new Insets(12, 18, 12, 18);
    private static final Insets ABA_COMPACTA = new Insets(8, 14, 8, 14);
    private static final Insets NENHUM = new Insets(0, 0, 0, 0);

    public static ComponentUI createUI(JComponent c) {
        return new AbasUI();
    }

    private boolean compactas() {
        return Boolean.TRUE.equals(tabPane.getClientProperty(Estilo.ABAS_COMPACTAS));
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabPane.setOpaque(false);
    }

    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return compactas() ? ABA_COMPACTA : ABA;
    }

    @Override
    protected Insets getTabAreaInsets(int tabPlacement) {
        return compactas() ? new Insets(0, 8, 0, 8) : new Insets(0, 14, 0, 14);
    }

    @Override
    protected Insets getSelectedTabPadInsets(int tabPlacement) {
        return NENHUM;
    }

    @Override
    protected Insets getContentBorderInsets(int tabPlacement) {
        return NENHUM;
    }

    @Override
    protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }

    @Override
    protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
        return 0;
    }

    @Override
    protected void setRolloverTab(int index) {
        int antes = getRolloverTab();
        super.setRolloverTab(index);
        if (antes != index) {
            tabPane.repaint();
        }
    }

    @Override
    protected void paintTabArea(Graphics g0, int tabPlacement, int selectedIndex) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            int altura = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
            if (!compactas()) {
                g.setColor(Paleta.CARTAO);
                g.fillRect(0, 0, tabPane.getWidth(), altura);
            }
            g.setColor(Paleta.BORDA);
            g.fillRect(0, altura - 1, tabPane.getWidth(), 1);
        } finally {
            g.dispose();
        }
        super.paintTabArea(g0, tabPlacement, selectedIndex);
    }

    @Override
    protected void paintTabBackground(Graphics g0, int tabPlacement, int tabIndex, int x, int y, int w, int h,
                                      boolean isSelected) {
        if (isSelected || getRolloverTab() != tabIndex || !tabPane.isEnabledAt(tabIndex)) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            g.setColor(Paleta.HOVER);
            g.fill(new RoundRectangle2D.Double(x + 4, y + 6, w - 8, h - 12, 8, 8));
        } finally {
            g.dispose();
        }
    }

    @Override
    protected void paintTabBorder(Graphics g0, int tabPlacement, int tabIndex, int x, int y, int w, int h,
                                  boolean isSelected) {
        if (!isSelected) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            g.clipRect(x, y, w, h);
            int folga = compactas() ? 8 : 12;
            g.setColor(Paleta.TEXTO);
            g.fill(new RoundRectangle2D.Double(x + folga, y + h - 3, w - 2 * folga, 6, 3, 3));
        } finally {
            g.dispose();
        }
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex,
                                       Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        // o sublinhado já mostra qual aba está escolhida
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        // sem moldura em volta do conteúdo
    }
}
