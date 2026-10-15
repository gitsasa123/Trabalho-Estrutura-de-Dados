package arvores.gui.visual;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuItemUI;

/** Itens de menu com o destaque arredondado. */
public final class MenuItemUI extends BasicMenuItemUI {

    public static ComponentUI createUI(JComponent c) {
        return new MenuItemUI();
    }

    @Override
    protected void paintBackground(Graphics g, JMenuItem item, Color corDeSelecao) {
        destacar(g, item);
    }

    /** Fundo do item (se opaco) e, se o mouse está sobre ele ou o menu está aberto, uma pílula clara. */
    static void destacar(Graphics g0, JMenuItem item) {
        ButtonModel m = item.getModel();
        Graphics2D g = (Graphics2D) g0.create();
        try {
            Paleta.qualidade(g);
            int w = item.getWidth();
            int h = item.getHeight();
            if (item.isOpaque()) {
                g.setColor(item.getBackground());
                g.fillRect(0, 0, w, h);
            }
            if (item.isEnabled() && (m.isArmed() || (item instanceof JMenu && m.isSelected()))) {
                g.setColor(Paleta.HOVER);
                boolean daBarra = item instanceof JMenu menu && menu.isTopLevelMenu();
                double folga = daBarra ? 2 : 4;
                g.fill(new RoundRectangle2D.Double(folga, daBarra ? 3 : 1, w - 2 * folga, h - (daBarra ? 6 : 2), 8, 8));
            }
        } finally {
            g.dispose();
        }
    }
}
